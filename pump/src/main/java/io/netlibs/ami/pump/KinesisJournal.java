package io.netlibs.ami.pump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.netlibs.ami.pump.utils.AggRecord;
import io.netlibs.ami.pump.utils.RecordAggregator;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.TimingPauser;
import net.openhft.chronicle.wire.DocumentContext;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

public class KinesisJournal extends AbstractExecutionThreadService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KinesisJournal.class);

  private SingleChronicleQueue queue;
  private AwsCredentialsProvider credentialsProvider;
  private String sequenceNumberForOrdering = "0";
  private Region region;
  private String partitionKey;
  private String streamName;
  private MeterRegistry compositeRegistry;
  private String pumpId;
  private KinesisClient kinesis;
  private TimingPauser pauser;
  private EventFilter filter;
  private Path path;
  private AtomicLong latestIndex = new AtomicLong();

  public KinesisJournal(
      Path dataroot,
      UpstreamConfig config,
      String pumpId,
      AwsCredentialsProvider credentialsProvider,
      MeterRegistry registry,
      Region region,
      StsClient stsClient) {

    this.pumpId = pumpId;
    this.region = region;

    this.path = dataroot.resolve(Paths.get(Optional.ofNullable(config.journalPath).orElse(config.streamName))).toAbsolutePath();

    if (!Files.isDirectory(path)) {
      try {
        Files.createDirectories(path);
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        throw new RuntimeException(e);
      }
    }

    RollCycles rollCycle = RollCycles.FIVE_MINUTELY;

    log.info("opening store at {}: {}", this.path, rollCycle);

    this.queue =
      ChronicleQueue.singleBuilder(path)
        .rollCycle(rollCycle)
        .readOnly(false)
        .build();

    try (ExcerptTailer tailer = queue.createTailer()) {
      tailer.toEnd();
      long next = tailer.index();
      if ((next > 0) && (next != Long.MAX_VALUE)) {
        this.latestIndex.set(next);
      }
      log.info("latestIndex is {}", next, this.latestIndex.get());
    }

    this.credentialsProvider =
      Optional.ofNullable(config.assumeRole)
        .map(roleArn -> (AwsCredentialsProvider) StsAssumeRoleCredentialsProvider.builder()
          .refreshRequest(b -> b.roleArn(roleArn).roleSessionName("ami2kinesis"))
          .asyncCredentialUpdateEnabled(true)
          .stsClient(stsClient)
          .build())
        .orElse(credentialsProvider);

    this.compositeRegistry = registry;
    this.streamName = config.streamName;
    this.partitionKey = Optional.ofNullable(config.partitionKey).orElse(this.pumpId);

    this.pauser = Pauser.balanced();

    this.kinesis =
      KinesisClient.builder()
        .credentialsProvider(this.credentialsProvider)
        .region(this.region)
        // we continue retrying forever. monitoring will pick up overly large queues.
        .overrideConfiguration(b -> b.retryPolicy(r -> r.numRetries(Integer.MAX_VALUE)))
        .build();

    this.filter = new EventFilter(Arrays.asList(config.filter));

    //

    this.compositeRegistry.gauge(
      "enqueued",
      ImmutableList.of(Tag.of("pump", this.pumpId), Tag.of("stream", this.streamName)),
      this,
      t -> queueSize());

  }

  @Override
  protected String serviceName() {
    return getClass().getSimpleName() + "-" + streamName;
  }

  public int readerCycle() {
    long index = this.queue.lastIndexReplicated();
    if (index == -1) {
      return 0;
    }
    if (index == 0) {
      return 0;
    }
    return queue.rollCycle().toCycle(index);
  }

  long queueSize() {

    long lowerIndex = queue.lastIndexReplicated();

    if (lowerIndex == -1) {
      return 0;
    }

    long currentIndex = latestIndex.get();

    if (lowerIndex >= currentIndex) {
      return 0;
    }

    return queue.countExcerpts(lowerIndex, currentIndex);

  }

  @Override
  protected void triggerShutdown() {
    this.pauser.unpause();
  }

  @Override
  protected void run() throws Exception {

    //
    ExcerptTailer tailer = queue.createTailer();

    long index = queue.lastIndexReplicated();

    //

    if (index > 0) {

      // note that we don't need to actually have a valid index, just want to be at least at this
      // index. a read will move us to the next one.
      tailer.moveToIndex(index);

    }
    else {

      // no current position, so move to the start.
      tailer.toStart();

    }

    log.info("commited index for {} is {} with latest index at {} - {} events buffered.", this.streamName, index, latestIndex, queueSize());

    while (this.isRunning()) {
      if (tryRead(tailer)) {
        pauser.reset();
      }
      else {
        pauser.pause();
      }
    }

  }

  public void append(String friendlyName, String json) {

    if (!this.filter.test(friendlyName)) {
      return;
    }

    ExcerptAppender appender = queue.acquireAppender();

    try (final DocumentContext dc = appender.writingDocument()) {
      dc.wire().write().text(json);
    }

    long lastAppended = appender.lastIndexAppended();
    this.latestIndex.getAndUpdate(val -> Math.max(val, lastAppended));

    pauser.unpause();

  }

  /**
   * 
   * @param tailer
   * @param kinesis
   * @param commitedIndex
   * @return
   */

  private boolean tryRead(ExcerptTailer tailer) {

    long position = queue.lastIndexReplicated();

    // commitedIndex.getVolatileValue();

    // move the tailer back to the commited index.
    if (position > 0) {
      tailer.moveToIndex(position);
    }
    else {
      tailer.toStart();
    }

    try {

      RecordAggregator agg = new RecordAggregator();

      agg.onRecordComplete(
        record -> flush(record, tailer.index()),
        MoreExecutors.directExecutor());

      while (this.isRunning()) {

        String nextDoc = readDoc(tailer);

        if (nextDoc == null) {
          break;
        }

        agg.addUserRecord(this.partitionKey, nextDoc.getBytes(StandardCharsets.UTF_8));

      }

      if (agg.getNumUserRecords() > 0) {
        flush(agg.clearAndGet(), tailer.index());
      }

      return false;

    }
    catch (SdkException ex) {

      // this is an AWS exception. we continue processing.
      String errorCode = getErrorCode(ex);

      log.warn("AWS SDK error: {}", errorCode, ex.getMessage());

      try {

        if (ex.retryable()) {
          // we want to retry pretty much straight away.
          Thread.sleep(Duration.ofMillis(10).toMillis());
          return true;
        }

        // this is a non retryable error, so we sleep before returning.
        Thread.sleep(Duration.ofSeconds(1).toMillis());

      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      return true;

    }
    catch (Exception ex) {

      log.error("propogating unknown error: {}", ex.getMessage(), ex);
      throw new RuntimeException(ex);

    }

  }

  private String readDoc(ExcerptTailer tailer) {
    try (DocumentContext dc = tailer.readingDocument()) {
      if (dc.isPresent() && !dc.isNotComplete()) {
        return dc.wire().read().text();
      }
    }
    return null;
  }

  private boolean flush(AggRecord agg, long nextIndex) {

    try {
      Stopwatch start = Stopwatch.createStarted();

      PutRecordResponse res =
        kinesis.putRecord(
          b -> b
            .streamName(this.streamName)
            .partitionKey(agg.getPartitionKey())
            .sequenceNumberForOrdering(this.sequenceNumberForOrdering)
            .data(SdkBytes.fromByteArray(agg.toRecordBytes())));

      start.stop();

      // this is only reached if we put sucessfully.

      log.debug("put {} user records in {}, seq {}", agg.getNumUserRecords(), start.elapsed(), res.sequenceNumber());

      this.sequenceNumberForOrdering = res.sequenceNumber();

      queue.lastIndexReplicated(nextIndex);

      this.compositeRegistry
        .timer("putrecord.success", "pump", this.pumpId, "stream", this.streamName, "shardId", res.shardId())
        .record(start.elapsed());

      this.compositeRegistry
        .counter("records.count", "pump", this.pumpId, "stream", this.streamName, "shardId", res.shardId())
        .increment(agg.getNumUserRecords());

      return true;

    }
    catch (SdkException ex) {

      String errorCode = getErrorCode(ex);

      this.compositeRegistry
        .counter("putrecord.error", "pump", this.pumpId, "stream", this.streamName, "error", errorCode)
        .increment();

      throw ex;

    }

  }

  private String getErrorCode(SdkException ex) {
    try {
      Throwables.throwIfInstanceOf(ex, AwsServiceException.class);
      return ex.getClass().toString();
    }
    catch (AwsServiceException awsex) {
      return "aws:" + awsex.awsErrorDetails().serviceName() + ":" + awsex.awsErrorDetails().errorCode();
    }
  }

  public SingleChronicleQueue journal() {
    return this.queue;
  }

  public long commitedIndex() {
    return this.queue.lastIndexReplicated();
  }

  public long latestIndex() {
    return this.latestIndex;
  }

}
