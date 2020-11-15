package io.netlibs.ami.pump;

import java.io.FileReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import javax.net.ssl.SSLContext;

import org.ini4j.Ini;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.UnsignedInts;
import com.google.common.util.concurrent.MoreExecutors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.netlibs.ami.api.AmiFrame;
import io.netlibs.ami.client.AmiConnection;
import io.netlibs.ami.client.AmiCredentials;
import io.netlibs.ami.client.ImmutableAmiCredentials;
import io.netlibs.ami.netty.DefaultAmiFrame;
import io.netlibs.ami.pump.event.ImmutableKinesisEvent;
import io.netlibs.ami.pump.event.KinesisEvent;
import io.netlibs.ami.pump.model.ImmutablePumpId;
import io.netlibs.ami.pump.model.ImmutableSourceInfo;
import io.netlibs.ami.pump.utils.AggRecord;
import io.netlibs.ami.pump.utils.ObjectMapperFactory;
import io.netlibs.ami.pump.utils.RecordAggregator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

public class Main implements Callable<Integer> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Main.class);

  // if not specified, where to search.
  private static final ImmutableList<String> managerSerchPaths =
    ImmutableList.of(
      "manager.conf",
      "/etc/asterisk/manager.conf",
      "/usr/local/etc/asterisk/manager.conf",
      "/usr/local/asterisk/conf/manager.conf");

  @Option(names = { "-u" }, description = "AMI username")
  private String username = "asterisk";

  @Option(names = { "-p" }, description = "AMI password")
  private String password;

  @Option(names = { "-f" }, description = "path to asterisk manager.conf to read credentails from")
  private Path configPath;

  @Option(names = { "-t" }, description = "target to connect to", defaultValue = "localhost")
  private String targetHost;

  @Option(names = { "-i" }, description = "instance identifier")
  private String instanceId;

  @Option(names = { "-j" }, description = "journal path")
  private String journalPath = "ami-journal";

  @Option(names = { "-s" }, description = "AWS Kinesis stream name to write to", defaultValue = "ami-events")
  private String streamName;

  @Option(names = { "-k" }, description = "AWS Kinesis partition to write to (default uses SystemName in frames)")
  private Optional<String> partitionKey = Optional.empty();

  @Option(names = { "--ping-interval" }, description = "keepalive interval (in seconds).", defaultValue = "PT5S")
  private Duration pingInterval;

  @Option(names = { "--read-timeout" }, description = "read idle seconds to wait for events before terminating.", defaultValue = "PT15S")
  private Duration readIdle;

  @Option(names = { "--ignore-events" }, description = "events to ignore (comma seperated).")
  private List<String> ignoreEventsInput = new ArrayList<>();

  @Option(names = { "--sns-control" }, description = "post control events to this sns topic arn.")
  private String snsControlEvents;

  @Option(names = { "--sns-attr-prefix" }, description = "message attribute prefix")
  private Optional<String> messageAttrPrefix = Optional.empty();

  private SnsAsyncClient sns;

  private ImmutableSet<String> ignoreEvents;

  private @NotNull SingleChronicleQueue queue;

  private Region region;

  private DefaultCredentialsProvider credentialsProvider;

  private String sequenceNumberForOrdering = "0";

  private MeterRegistry compositeRegistry;

  public HostAndPort target() {

    HostAndPort tt = HostAndPort.fromString(this.targetHost);

    if (tt.hasPort()) {
      return tt;
    }

    if (configPath == null) {
      return tt.withDefaultPort(5030);
    }

    try {
      // get port number from config too.
      Ini ini = new Ini();
      ini.load(new FileReader(configPath.toFile()));
      Ini.Section general = ini.get("general");
      return tt.withDefaultPort(UnsignedInts.parseUnsignedInt(general.get("port")));
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public Integer call() throws Exception {

    if (this.configPath != null) {
      if (!Files.exists(this.configPath)) {
        throw new IllegalArgumentException(String.format("specified config file %s does not exist", this.configPath));
      }
    }

    // try to find automatically.
    if (this.password == null) {

      if (this.configPath == null) {

        for (String s : managerSerchPaths) {
          this.configPath = Paths.get(s);
          if (Files.exists(this.configPath)) {
            break;
          }

        }

        if (!Files.exists(this.configPath)) {
          throw new IllegalArgumentException(String.format("no password specified, but can't find asterisk manager config file."));
        }

      }

    }

    this.ignoreEvents =
      this.ignoreEventsInput.stream()
        .flatMap(e -> Arrays.stream(e.split(",")))
        .map(val -> val.toLowerCase().trim())
        .filter(e -> e.length() > 0)
        .collect(ImmutableSet.toImmutableSet());

    journalPath = Paths.get(journalPath).toAbsolutePath().toString();
    log.info("using path: {}", journalPath);

    this.compositeRegistry = new LoggingMeterRegistry();

    this.queue =
      ChronicleQueue.singleBuilder(journalPath)
        .rollCycle(RollCycles.HOURLY)
        .build();

    log.info("ignoring events: {} (from input {})", this.ignoreEvents, this.ignoreEventsInput);

    this.region = new software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain().getRegion();
    this.credentialsProvider = DefaultCredentialsProvider.create();

    log.info("supported SSL protocols: {}", Arrays.toString(SSLContext.getDefault().getSupportedSSLParameters().getProtocols()));
    log.info("supported SSL ciphers: {}", Arrays.toString(SSLContext.getDefault().getSupportedSSLParameters().getCipherSuites()));

    //

    if (!Strings.isNullOrEmpty(this.snsControlEvents)) {
      this.sns =
        SnsAsyncClient.builder()
          .credentialsProvider(credentialsProvider)
          .region(region)
          .asyncConfiguration(b -> b.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Executors.newFixedThreadPool(2)))
          .build();
    }

    //
    ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    //
    ImmutablePumpId pumpId = ImmutablePumpId.of(getStableInstanceID(), System.currentTimeMillis());
    ObjectNode pump = JsonNodeFactory.instance.objectNode();
    pump.put("id", pumpId.id());
    pump.put("epoch", pumpId.epoch());

    //
    AtomicLong seqno = new AtomicLong();

    notifyInit("INIT", pumpId);

    NioEventLoopGroup eventLoop = new NioEventLoopGroup(1);

    try {

      CompletableFuture<Channel> connector = AmiConnection.connect(eventLoop, target(), Duration.ofSeconds(5));

      // includes negotiation
      Channel ch = connector.get(10, TimeUnit.SECONDS);

      log.info("connected to '{}'", pumpId.id());

      ImmutableAmiCredentials credentials = this.credentials();

      AtomicLong nextActionId = new AtomicLong();

      DefaultAmiFrame loginFrame = DefaultAmiFrame.newFrame();
      loginFrame.add("Action", "Login");
      loginFrame.add("ActionID", Long.toHexString(nextActionId.incrementAndGet()));
      loginFrame.add("Username", credentials.username());
      loginFrame.add("Secret", credentials.secret());
      ch.writeAndFlush(loginFrame);

      if ((this.pingInterval != null) || (this.readIdle != null)) {
        ch.pipeline().addLast(new IdleStateHandler(this.readIdle.toMillis(), 0, this.pingInterval.toMillis(), TimeUnit.MILLISECONDS));
      }

      ch.pipeline()
        .addLast(new SimpleChannelInboundHandler<Object>(true) {

          boolean closed = false;

          @Override
          public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
              IdleStateEvent e = (IdleStateEvent) evt;
              log.info("idle event {}", e);
              if (e.state() == IdleState.READER_IDLE) {
                log.info("reader was idle for too long ({}), closing connection", readIdle);
                this.closed = true;
                ctx.close();
              }
              else if (e.state() == IdleState.ALL_IDLE) {
                // no read or writes for the interval, so send ping.
                log.info("no read or write for {}, sending ping", pingInterval);
                sendPing(ctx);
              }
            }
          }

          private void sendPing(ChannelHandlerContext ctx) {
            log.info("sending ping");
            DefaultAmiFrame pingFrame = DefaultAmiFrame.newFrame();
            pingFrame.add("Action", "Ping");
            pingFrame.add("ActionID", Long.toHexString(nextActionId.incrementAndGet()));
            ctx.writeAndFlush(pingFrame);
          }

          @Override
          protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

            if (msg instanceof AmiFrame) {

              AmiFrame frame = (AmiFrame) msg;

              CharSequence res = frame.get("Response");

              if (res != null) {

                if (res.equals("Error")) {
                  log.error("got aim frame error: {}", frame);
                  ctx.channel().close();
                  closed = true;
                  return;
                }

                // note: we include the Ping response in the stream. this is useful for keepalive
                // purposes.

              }
              else {

                String eventType = frame.getOrDefault("Event", "").toString();

                if (eventType.isEmpty()) {
                  log.error("invalid Event frame (missing Event): {}", frame);
                  return;
                }

                if (ignoreEvents.contains(eventType.toLowerCase())) {
                  log.debug("ignoring event: {}", eventType);
                  return;
                }

                try {

                  String friendlyEvent = eventType;

                  if (eventType.equalsIgnoreCase("UserEvent")) {
                    friendlyEvent = frame.getOrDefault("UserEvent", "").toString();
                  }

                  compositeRegistry
                    .counter("ami2kinesis.events.count", "pump", instanceId, "type", friendlyEvent.toLowerCase())
                    .increment();

                }
                catch (Exception ex) {
                  log.warn("error recording event", ex.getMessage());
                }

              }

              KinesisEvent evt = convert(pumpId.id(), seqno, frame);
              ObjectNode e = convert(evt, pump);
              String output = mapper.writeValueAsString(e) + "\n";

              appendJournal(output);

            }

          }

          @Override
          public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("channel received exception: {}", cause.getMessage(), cause);
            if (ctx.channel().isOpen() && !closed) {
              log.info("closing channel");
              closed = true;
              ctx.channel().close();
            }
          }

          @Override
          public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            if (!closed && ctx.channel().isActive()) {
              // always read more, as long as we are not closed.
              ctx.read();
            }
          }

        });

      // fire off an initial read.
      ch.read();

      writeToKinesis(ch, credentialsProvider, queue);

      log.info("channel closed");

    }
    catch (Throwable e) {

      log.error("error pumping events", e.getMessage(), e);

    }
    finally {

      log.info("shutting down event loop");

      try {
        eventLoop.shutdownGracefully(5, 5, TimeUnit.SECONDS).get(30, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        log.error("failed to gracefully shut event loop down");
      }

    }

    log.info("exiting");
    // hard exit, avoid background threads blocking us
    System.exit(0);
    return 0;

  }

  private void writeToKinesis(Channel ch, AwsCredentialsProvider credentialsProvider, SingleChronicleQueue queue) {

    //
    ChannelFuture closed = ch.closeFuture();
    ExcerptTailer tailer = queue.createTailer();

    Pauser pauser = Pauser.balanced();

    closed.addListener(new GenericFutureListener<Future<? super Void>>() {

      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {
        pauser.unpause();
      }

    });

    LongValue commitedIndex = queue.metaStore().acquireValueFor("kinesis.position", 0);

    long index = commitedIndex.getVolatileValue();

    log.info("commited index is {}", index);

    if (index > 0) {
      // note that we don't need to actually have a valid index, just want to be at least at this
      // index. a read will move us to the next one.
      tailer.moveToIndex(index);
    }

    KinesisClient kinesis =
      KinesisClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(this.region)
        // we continue retrying forever. monitoring will pick up overly large queues.
        .overrideConfiguration(b -> b.retryPolicy(r -> r.numRetries(Integer.MAX_VALUE)))
        .build();

    while (!closed.isDone()) {
      if (tryRead(tailer, kinesis, commitedIndex, () -> !closed.isDone())) {
        pauser.reset();
      }
      else {
        pauser.pause();
      }
    }

  }

  private void appendJournal(String json) {

    if (queue == null) {
      return;
    }

    ExcerptAppender appender = queue.acquireAppender();

    try (final DocumentContext dc = appender.writingDocument()) {
      dc.wire().write().text(json);
    }

  }

  /**
   * 
   * @param tailer
   * @param kinesis
   * @param commitedIndex
   * @return
   */

  private boolean tryRead(ExcerptTailer tailer, KinesisClient kinesis, LongValue commitedIndex, BooleanSupplier active) {

    RecordAggregator agg = new RecordAggregator();

    agg.onRecordComplete(
      record -> flush(kinesis, record, tailer.index(), commitedIndex),
      MoreExecutors.directExecutor());

    while (active.getAsBoolean()) {

      String nextDoc = readDoc(tailer);

      if (nextDoc == null) {
        break;
      }

      try {
        agg.addUserRecord(this.instanceId, nextDoc.getBytes(StandardCharsets.UTF_8));
      }
      catch (Exception e) {
        log.error("failed to add user records, aborting process.");
        Runtime.getRuntime().halt(1);
      }

    }

    if (agg.getNumUserRecords() > 0) {
      flush(kinesis, agg.clearAndGet(), tailer.index(), commitedIndex);
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

  private void flush(KinesisClient kinesis, AggRecord agg, long nextIndex, LongValue commitedIndex) {

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
          .timer("ami2kinesis.putrecord.duration", "pump", this.instanceId, "shardId", res.shardId())
          .record(start.elapsed());

        this.compositeRegistry
          .counter("ami2kinesis.records.count", "pump", this.instanceId, "shardId", res.shardId())
          .increment(agg.getNumUserRecords());

        this.compositeRegistry
          .counter("ami2kinesis.putrecord.count", "pump", this.instanceId, "shardId", res.shardId())
          .increment();

        return;
      }
      catch (Exception ex) {
        log.warn("failed to call PutRecord", ex.getMessage());
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ex1) {
          Runtime.getRuntime().halt(1);
        }
      }

    }

  }

  private void notifyInit(String string, ImmutablePumpId pumpId) {
    if (this.sns == null) {
      return;
    }
    try {
      String prefix = this.messageAttrPrefix.map(val -> Strings.nullToEmpty(val)).orElse("");
      ObjectNode metadata = JsonNodeFactory.instance.objectNode();
      this.sns.publish(req -> req
        .topicArn(this.snsControlEvents)
        .messageAttributes(ImmutableMap
          .of(
            String.format("%sPumpEvent", prefix),
            MessageAttributeValue.builder()
              .dataType("String")
              .stringValue("INIT")
              .build(),
            String.format("%sPumpId", prefix),
            MessageAttributeValue.builder()
              .dataType("String")
              .stringValue(pumpId.id())
              .build(),
            String.format("%sPumpEpoch", prefix),
            MessageAttributeValue.builder()
              .dataType("Number")
              .stringValue(Long.toString(pumpId.epoch()))
              .build()
          //
          ))
        .message(metadata.toString()))
        .handle((snsres, err) -> {
          if (err != null) {
            log.info("failed to notify SNS", err.getMessage(), err);
          }
          else {
            log.info("published to SNS, messageId {}", snsres.messageId());
          }
          return null;
        })
        .get(5, TimeUnit.SECONDS);
    }
    catch (Exception ex) {
      log.warn("failed to notify SNS: {}", ex.getMessage(), ex);
      // don't exit on failure here, ignore but log.
    }
  }

  private ImmutableAmiCredentials credentials() {

    if ((configPath == null) || (this.password != null)) {
      return AmiCredentials.of(this.username, this.password);
    }

    try {
      Ini ini = new Ini();
      ini.load(new FileReader(configPath.toFile()));
      Ini.Section section = ini.get(username);
      if (section == null) {
        throw new IllegalArgumentException(String.format("config file [%s] does not contain user '%s'", configPath, this.username));
      }
      return AmiCredentials.of(section.getName(), section.get("secret"));
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  private static KinesisEvent convert(String sourceId, AtomicLong seqno, AmiFrame event) {

    ObjectNode jevt = JsonNodeFactory.instance.objectNode();
    event.forEach((k, v) -> jevt.put(k.toString(), v.toString()));

    // do not pass this header on.
    jevt.remove("Privilege");

    // remove SystemName if it matches the source id.
    String source = event.getOrDefault("SystemName", sourceId);

    if (event.scalarContains("SystemName", source)) {
      jevt.remove("SystemName");
    }

    return ImmutableKinesisEvent.builder()
      .source(ImmutableSourceInfo.of(sourceId, seqno.getAndIncrement()))
      .payload(jevt)
      .build();

  }

  private static ObjectNode convert(KinesisEvent e, ObjectNode pump) {
    ObjectNode vals = JsonNodeFactory.instance.objectNode();
    vals.put("v", 2);
    vals.set("pump", pump.deepCopy());
    vals.put("typ", "event");
    vals.put("at", Instant.now().toString());
    ObjectNode sourceNode = vals.putObject("src");
    sourceNode.put("id", e.source().sourceId());
    sourceNode.put("seq", e.source().sourceSequence());
    vals.set("event", e.payload());
    return vals;
  }

  private String getStableInstanceID() {
    if ((this.instanceId != null) && !this.instanceId.isEmpty()) {
      return this.instanceId;
    }
    try {
      InetAddress inetaddress = InetAddress.getLocalHost();
      return inetaddress.getHostAddress();
    }
    catch (Exception err) {
      throw new RuntimeException(err);
    }
  }

  public static void main(String[] args) throws Exception {
    new CommandLine(new Main()).execute(args);
  }

}
