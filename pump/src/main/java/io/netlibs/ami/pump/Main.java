package io.netlibs.ami.pump;

import java.io.FileReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.ini4j.Ini;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HostAndPort;
import com.google.common.primitives.UnsignedInts;

import io.netlibs.ami.api.AmiFrame;
import io.netlibs.ami.client.AmiConnection;
import io.netlibs.ami.client.AmiCredentials;
import io.netlibs.ami.client.ImmutableAmiCredentials;
import io.netlibs.ami.netty.DefaultAmiFrame;
import io.netlibs.ami.pump.event.ImmutableKinesisEvent;
import io.netlibs.ami.pump.event.KinesisEvent;
import io.netlibs.ami.pump.model.ImmutablePumpId;
import io.netlibs.ami.pump.model.ImmutableSourceInfo;
import io.netlibs.ami.pump.utils.KinesisClient;
import io.netlibs.ami.pump.utils.ObjectMapperFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class Main implements Callable<Integer> {

  @Option(names = { "-u" }, description = "AMI username")
  private String username = "asterisk";

  @Option(names = { "-p" }, description = "AMI password")
  private String password = "asterisk";

  @Option(names = { "-f" }, description = "path to asterisk manager.conf to read credentails from")
  private Path configPath;

  @Option(names = { "-t" }, description = "target to connect to", defaultValue = "localhost")
  private String targetHost;

  @Option(names = { "-s" }, description = "AWS Kinesis stream name to write to", defaultValue = "ami-events")
  private String streamName;

  @Option(names = { "-k" }, description = "AWS Kinesis partition to write to (default uses SystemName in frames)")
  private Optional<String> partitionKey = Optional.empty();

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

    //

    //

    AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();

    KinesisClient kinesis = new KinesisClient(new AWSCredentialsProvider() {

      @Override
      public void refresh() {
      }

      @Override
      public AWSCredentials getCredentials() {
        AwsSessionCredentials creds = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
        return new BasicSessionCredentials(creds.accessKeyId(), creds.secretAccessKey(), creds.sessionToken());
      }

    }, new software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain().getRegion().id(), streamName);

    //
    ImmutablePumpId pumpId = ImmutablePumpId.of(getStableInstanceID(), System.currentTimeMillis());
    ObjectNode pump = JsonNodeFactory.instance.objectNode();
    pump.put("id", pumpId.id());
    pump.put("epoch", pumpId.epoch());
    //

    NioEventLoopGroup eventLoop = new NioEventLoopGroup(1);

    CompletableFuture<Channel> connector = AmiConnection.connect(eventLoop, target(), Duration.ofSeconds(5));

    ObjectMapper mapper = ObjectMapperFactory.objectMapper();

    // includes negotiation
    Channel ch = connector.get(10, TimeUnit.SECONDS);

    System.err.println("connected");

    ImmutableAmiCredentials credentials = this.credentials();

    DefaultAmiFrame loginFrame = DefaultAmiFrame.newFrame();
    loginFrame.add("Action", "Login");
    loginFrame.add("ActionID", Long.toHexString(1));
    loginFrame.add("Username", credentials.username());
    loginFrame.add("Secret", credentials.secret());
    ch.writeAndFlush(loginFrame);

    AtomicLong seqno = new AtomicLong();

    ch.pipeline().addLast(new SimpleChannelInboundHandler<Object>(true) {

      boolean closed = false;

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {

          if (msg instanceof AmiFrame) {

            AmiFrame frame = (AmiFrame) msg;
            System.err.println(msg);

            CharSequence res = frame.get("Response");

            if (res != null) {

              if (res.equals("Error")) {
                ctx.channel().close();
                closed = true;
                return;
              }

            }

            try {
              ObjectNode e = convert(convert(pumpId.id(), seqno, frame), pump);
              String output = mapper.writeValueAsString(e) + "\n";
              String partitionName = frame.getOrDefault("SystemName", "unknown").toString();
              kinesis.add(partitionName, ByteBuffer.wrap(output.getBytes(StandardCharsets.UTF_8)));
            }
            catch (JsonProcessingException e) {
              e.printStackTrace();
            }

          }

        }

        finally {

          // always read more.
          if (!closed)
            ctx.read();
        }

      }

    });

    try {
      while (ch.isOpen()) {
        ch.read();
      }
      System.err.println("closed");
    }
    finally {
      kinesis.flushSync();
      kinesis.close();
      eventLoop.shutdownGracefully();
    }

    System.err.println("done");

    return 0;

  }

  private ImmutableAmiCredentials credentials() {
    if ((configPath == null) || (this.password != null)) {
      return AmiCredentials.of(this.username, this.password);
    }
    try {
      Ini ini = new Ini();
      ini.load(new FileReader(configPath.toFile()));
      Ini.Section section = ini.get(username);
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
      .source(ImmutableSourceInfo.of(sourceId, seqno.incrementAndGet()))
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

  private static String getStableInstanceID() {
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
