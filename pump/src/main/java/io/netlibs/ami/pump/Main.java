package io.netlibs.ami.pump;

import java.io.FileReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ini4j.Ini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.UnsignedInts;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import io.netlibs.ami.netty.DefaultAmiFrame;
import io.netlibs.ami.pump.model.ImmutablePumpId;
import io.netlibs.ami.pump.utils.ObjectMapperFactory;
import io.netlibs.asterisk.ami.client.AmiConnection;
import io.netlibs.asterisk.ami.client.AmiCredentials;
import io.netlibs.asterisk.ami.client.ImmutableAmiCredentials;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.IdleStateHandler;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sts.StsClient;

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
  private final String username = "asterisk";

  @Option(names = { "-p" }, description = "AMI password")
  private String password;

  @Option(names = { "-f" }, description = "path to asterisk manager.conf to read credentials from")
  private Path configPath;

  @Option(names = { "-D" }, description = "data root path")
  private final Path dataRoot = Paths.get("ami2kinesis").toAbsolutePath();

  @Option(names = { "-t" }, description = "target to connect to", defaultValue = "localhost")
  private String targetHost;

  @Option(names = { "-i" }, description = "instance identifier")
  private String instanceId;

  @Option(names = { "--ping-interval" }, description = "keepalive interval (in seconds).", defaultValue = "PT5S")
  private Duration pingInterval;

  @Option(names = { "--read-timeout" }, description = "read idle seconds to wait for events before terminating.", defaultValue = "PT15S")
  private Duration readIdle;

  @Option(names = { "--ignore-events" }, description = "events to ignore (comma seperated).")
  private final List<String> ignoreEventsInput = new ArrayList<>();

  @Option(names = { "--sns-control" }, description = "post control events to this sns topic arn.")
  private String snsControlEvents;

  @Option(names = { "--sns-attr-prefix" }, description = "message attribute prefix")
  private final Optional<String> messageAttrPrefix = Optional.empty();

  //

  //
  @ArgGroup(exclusive = false, multiplicity = "1..*")
  private final List<UpstreamConfig> upstreams = new LinkedList<>();

  //
  private SnsAsyncClient sns;
  private ImmutableSet<String> ignoreEvents;
  private Region region;

  private CompositeMeterRegistry compositeRegistry;

  private DefaultCredentialsProvider credentialsProvider;

  private ImmutableList<KinesisJournal> streams;

  public HostAndPort target() {

    final HostAndPort tt = HostAndPort.fromString(this.targetHost);

    if (tt.hasPort()) {
      return tt;
    }

    if (this.configPath == null) {
      return tt.withDefaultPort(5030);
    }

    try {
      // get port number from config too.
      final Ini ini = new Ini();
      ini.load(new FileReader(this.configPath.toFile()));
      final Ini.Section general = ini.get("general");
      return tt.withDefaultPort(UnsignedInts.parseUnsignedInt(general.get("port")));
    }
    catch (final Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  @Override
  public Integer call() throws Exception {

    if ((this.configPath != null) && !Files.exists(this.configPath)) {
      throw new IllegalArgumentException(String.format("specified config file %s does not exist", this.configPath));
    }

    // dataRoot = dataRoot.t

    // try to find automatically.
    if ((this.password == null) && (this.configPath == null)) {

      for (final String s : managerSerchPaths) {
        this.configPath = Paths.get(s);
        if (Files.exists(this.configPath)) {
          break;
        }

      }

      if (!Files.exists(this.configPath)) {
        throw new IllegalArgumentException(String.format("no password specified, but can't find asterisk manager config file."));
      }

    }

    // events to ignore completely.
    this.ignoreEvents =
      this.ignoreEventsInput.stream()
        .flatMap(e -> Arrays.stream(e.split(",")))
        .map(val -> val.toLowerCase().trim())
        .filter(e -> e.length() > 0)
        .collect(ImmutableSet.toImmutableSet());

    // setup logging.
    this.compositeRegistry = new CompositeMeterRegistry();

    this.compositeRegistry.add(new LoggingMeterRegistry());

    final StatsdConfig config = new StatsdConfig() {

      @Override
      public String get(final String k) {
        return null;
      }

      @Override
      public StatsdFlavor flavor() {
        return StatsdFlavor.DATADOG;
      }

    };

    this.compositeRegistry.add(new StatsdMeterRegistry(config, Clock.SYSTEM));

    this.credentialsProvider = DefaultCredentialsProvider.create();
    this.region = new DefaultAwsRegionProviderChain().getRegion();

    log.info("ignoring events: {} (from input {})", this.ignoreEvents, this.ignoreEventsInput);

    // generate a map of streams we will be writing to.

    final StsClient stsClient =
      StsClient.builder()
        .region(this.region)
        .build();

    this.streams =
      this.upstreams.stream()
        .map(upstream -> new KinesisJournal(
          this.dataRoot,
          upstream,
          this.instanceId,
          this.credentialsProvider,
          this.compositeRegistry,
          this.region,
          stsClient))
        .collect(ImmutableList.toImmutableList());

    // streams

    final ArrayList<Service> services = new ArrayList<>();

    final CleanupService cleaner = new CleanupService(this.streams);

    services.addAll(this.streams);
    services.add(cleaner);

    final ServiceManager serviceManager = new ServiceManager(services);
    serviceManager.addListener(new AmiServiceManagerListener(), MoreExecutors.directExecutor());
    serviceManager.startAsync();

    // SNS control client.

    if (!Strings.isNullOrEmpty(this.snsControlEvents)) {
      this.sns =
        SnsAsyncClient.builder()
          .credentialsProvider(this.credentialsProvider)
          .region(this.region)
          .asyncConfiguration(b -> b.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Executors.newFixedThreadPool(2)))
          .build();
    }

    //
    final ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    //
    final ImmutablePumpId pumpId = ImmutablePumpId.of(this.getStableInstanceID(), System.currentTimeMillis());

    this.notifyInit("INIT", pumpId);

    final NioEventLoopGroup eventLoop = new NioEventLoopGroup(1);

    try {

      final CompletableFuture<Channel> connector = AmiConnection.connect(eventLoop, this.target(), Duration.ofSeconds(5));

      // includes negotiation, should be plenty of time for a well functioning system.
      final Channel ch = connector.get(10, TimeUnit.SECONDS);

      log.info("connected to '{}'", pumpId.id());

      final ImmutableAmiCredentials credentials = this.credentials();

      final AmiChannelHandler handler = new AmiChannelHandler(mapper, this.compositeRegistry, pumpId, this.ignoreEvents, this.streams);

      final DefaultAmiFrame loginFrame = DefaultAmiFrame.newFrame();
      loginFrame.add("Action", "Login");
      loginFrame.add("ActionID", Long.toHexString(handler.nextActionId()));
      loginFrame.add("Username", credentials.username());
      loginFrame.add("Secret", credentials.secret());

      ch.writeAndFlush(loginFrame);

      if ((this.pingInterval != null) || (this.readIdle != null)) {
        ch.pipeline().addLast(new IdleStateHandler(this.readIdle.toMillis(), 0, this.pingInterval.toMillis(), TimeUnit.MILLISECONDS));
      }

      //
      ch.pipeline().addLast(handler);

      // fire off an initial read.
      ch.read();

      // this thread just waits for the channel to close. once it does, we signal to the kinesis
      // uploaders to shutdown.
      ch.closeFuture().awaitUninterruptibly();

      log.info("channel closed");

    }
    catch (final Throwable e) {

      log.error("error pumping events", e.getMessage(), e);

    }
    finally {

      serviceManager.stopAsync();

      log.info("shutting down event loop");

      try {
        eventLoop.shutdownGracefully(5, 5, TimeUnit.SECONDS).get(30, TimeUnit.SECONDS);
      }
      catch (final Exception e) {
        log.error("failed to gracefully shut event loop down");
      }

      // wait max 30 seconds for the kinesis writers to shutdown and sync. it should never be
      // anywhere near this long.
      serviceManager.awaitStopped(30, TimeUnit.SECONDS);

    }

    log.info("exiting");
    // hard exit, avoid background threads blocking us
    System.exit(0);
    return 0;

  }

  private void notifyInit(final String string, final ImmutablePumpId pumpId) {
    if (this.sns == null) {
      return;
    }
    try {
      final String prefix = this.messageAttrPrefix.map(Strings::nullToEmpty).orElse("");
      final ObjectNode metadata = JsonNodeFactory.instance.objectNode();
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
    catch (final Exception ex) {
      log.warn("failed to notify SNS: {}", ex.getMessage(), ex);
      // don't exit on failure here, ignore but log.
    }
  }

  private ImmutableAmiCredentials credentials() {

    if ((this.configPath == null) || (this.password != null)) {
      return AmiCredentials.of(this.username, this.password);
    }

    try {
      final Ini ini = new Ini();
      ini.load(new FileReader(this.configPath.toFile()));
      final Ini.Section section = ini.get(this.username);
      if (section == null) {
        throw new IllegalArgumentException(String.format("config file [%s] does not contain user '%s'", this.configPath, this.username));
      }
      return AmiCredentials.of(section.getName(), section.get("secret"));
    }
    catch (final Exception ex) {
      throw new RuntimeException(ex);
    }

  }

  private String getStableInstanceID() {
    if ((this.instanceId != null) && !this.instanceId.isEmpty()) {
      return this.instanceId;
    }
    try {
      final InetAddress inetaddress = InetAddress.getLocalHost();
      return inetaddress.getHostAddress();
    }
    catch (final Exception err) {
      throw new RuntimeException(err);
    }
  }

  public static void main(final String[] args) throws Exception {
    new CommandLine(new Main()).execute(args);
  }

}
