package io.netlibs.ami.pump;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Optional;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.netlibs.ami.pump.utils.AggRecord;
import io.netlibs.ami.pump.utils.RecordAggregator;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueStore;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.TimingPauser;
import net.openhft.chronicle.wire.DocumentContext;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
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
  private StoreListener storeListener;
  private LongValue commitedIndex;

  private volatile long latestIndex;

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

    this.storeListener = new StoreListener(config.streamName, this::readerCycle);

    this.queue =
      ChronicleQueue.singleBuilder(path)
        .storeFileListener(this.storeListener)
        .rollCycle(RollCycles.FIVE_MINUTELY)
        .build();

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
    this.commitedIndex = queue.metaStore().acquireValueFor("kinesis.position", 0);

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

  public void cleanup() {

    try {

      long lowerIndex = queue.firstIndex();

      if (lowerIndex == Long.MAX_VALUE) {
        log.info("no clean as initial index is not present");
        return;
      }

      int lowerCycle = queue.rollCycle().toCycle(lowerIndex);
      int upperCycle = this.readerCycle() - 1;

      log.info("attemting cleanup on {} for cycles {} -> {}", streamName, lowerCycle, upperCycle);

      if (lowerCycle >= upperCycle) {
        return;
      }

      if ((upperCycle < queue.firstCycle()) || (upperCycle > queue.lastCycle())) {
        return;
      }

      NavigableSet<Long> cycles = queue.listCyclesBetween(lowerCycle, upperCycle);

      cycles.forEach(cycle -> {

        SingleChronicleQueueStore wireStore = queue.storeForCycle(cycle.intValue(), 0, false, null);

        if (wireStore != null) {
          File file = wireStore.file();
          wireStore.close();
          log.info("{} cycle {} no longer used, deleting {}", this.streamName, cycle, file.getAbsolutePath());
          file.delete();
        }

      });

    }
    catch (Exception e) {

      log.error("failed to cleanup {}: {}", streamName, e.getMessage());

    }

  }

  int readerCycle() {
    if (this.commitedIndex == null) {
      return 0;
    }
    long index = this.commitedIndex.getVolatileValue();
    if (index == 0) {
      return 0;
    }
    return queue.rollCycle().toCycle(index);
  }

  long queueSize() {

    long lowerIndex = commitedIndex.getVolatileValue();

    if ((lowerIndex == Long.MAX_VALUE) || (lowerIndex == 0)) {
      return 0;
    }

    if (lowerIndex >= latestIndex) {
      return 0;
    }

    return queue.countExcerpts(lowerIndex, latestIndex);

  }

  @Override
  protected void triggerShutdown() {
    this.pauser.unpause();
  }

  @Override
  protected void run() throws Exception {

    log.info("filter is: {}", this.filter);

    this.cleanup();

    //

    ExcerptTailer tailer = queue.createTailer();
    this.latestIndex = tailer.toEnd().index();

    long index = commitedIndex.getVolatileValue();

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

    long pendingEvents =
      tailer.index() >= latestIndex ? 0
                                    : queue.countExcerpts(tailer.index(), latestIndex);

    log.info("commited index for {} is {} with latest index at {} - {} events buffered.", this.streamName, index, latestIndex, pendingEvents);

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

    this.latestIndex = appender.lastIndexAppended();

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

    RecordAggregator agg = new RecordAggregator();

    agg.onRecordComplete(
      record -> flush(record, tailer.index(), commitedIndex),
      MoreExecutors.directExecutor());

    while (this.isRunning()) {

      String nextDoc = readDoc(tailer);

      if (nextDoc == null) {
        break;
      }

      try {
        agg.addUserRecord(this.partitionKey, nextDoc.getBytes(StandardCharsets.UTF_8));
      }
      catch (Exception e) {
        log.error("failed to add user records, aborting process.");
        Runtime.getRuntime().halt(1);
      }

    }

    if (agg.getNumUserRecords() > 0) {
      flush(agg.clearAndGet(), tailer.index(), commitedIndex);
    }

    return false;

  }

  private String readDoc(ExcerptTailer tailer) {
    try (DocumentContext dc = tailer.readingDocument()) {
      if (dc.isPresent() && !dc.isNotComplete()) {
        return dc.wire().read().text();
      }
    }
    return null;
  }

  private void flush(AggRecord agg, long nextIndex, LongValue commitedIndex) {

    Stopwatch start = Stopwatch.createStarted();

    while (true) {
      try {

        PutRecordResponse res =
          kinesis.putRecord(
            b -> b
              .streamName(this.streamName)
              .partitionKey(agg.getPartitionKey())
              .sequenceNumberForOrdering(this.sequenceNumberForOrdering)
              .data(SdkBytes.fromByteArray(agg.toRecordBytes())));

        start.stop();

        log.debug("put {} user records in {}, seq {}", agg.getNumUserRecords(), start.elapsed(), res.sequenceNumber());

        this.sequenceNumberForOrdering = res.sequenceNumber();

        commitedIndex.setVolatileValue(nextIndex);

        this.compositeRegistry
          .timer("putrecord", "pump", this.pumpId, "stream", this.streamName, "shardId", res.shardId())
          .record(start.elapsed());

        this.compositeRegistry
          .counter("records.count", "pump", this.pumpId, "stream", this.streamName, "shardId", res.shardId())
          .increment(agg.getNumUserRecords());

        return;
      }
      catch (Exception ex) {
        // this only happens after Integer.MAX_VALUE retries has been reached. uefg!
        log.warn("failed to call PutRecord: {}", ex.getMessage());
        Runtime.getRuntime().halt(1);
      }

    }

  }

}
