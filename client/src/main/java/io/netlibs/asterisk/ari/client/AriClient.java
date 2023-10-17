package io.netlibs.asterisk.ari.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.HostAndPort;

import io.netlibs.asterisk.ari.commands.OriginateParams;
import io.netlibs.asterisk.ari.events.Channel;
import io.netlibs.asterisk.ari.events.Event;

public class AriClient implements Ari {

  private static final Logger LOG = LoggerFactory.getLogger(AriClient.class);

  private static final ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

  private final HttpClient httpClient;

  private final AriTransport transport;

  private final URI baseUri;

  public AriClient(final String baseUri, final HttpClient httpClient) {
    this.baseUri = URI.create(baseUri);
    this.httpClient = httpClient;
    this.transport = new AriTransport(httpClient, baseUri);
  }

  public HttpResponse<String> createChannel(final Function<OriginateParams.EndpointBuildStage, OriginateParams.BuildFinal> params) throws InterruptedException {
    return this.createChannel(params.apply(OriginateParams.builder()).build());
  }

  /**
   * @throws InterruptedException
   *
   */

  public HttpResponse<String> createChannel(final OriginateParams params) throws InterruptedException {

    try {

      // convert to query parameters.
      final Map<String, String> args =
        mapper.convertValue(
          params,
          TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class));

      System.err.println(args);

      final URI uri = Utils.makeUri(this.baseUri, List.of("channels", "create"), args);

      record Payload(Map<String, String> variables) {
      }

      System.err.println(uri);

      final String body = mapper.writeValueAsString(new Payload(params.variables()));

      System.err.println(body);

      final HttpRequest req =
        HttpRequest.newBuilder()
          .uri(uri)
          .header("content-type", "application/json")
          .POST(BodyPublishers.ofString(body))
          .build();

      final var res = this.httpClient.send(req, BodyHandlers.ofString());

      if (res.statusCode() >= 300) {
        throw new IllegalArgumentException(res.body());
      }

      return res;

    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void hangup(final String channelId, final String reason) throws InterruptedException {
    final HashMap<String, String> args = new HashMap<>();
    if (reason != null) {
      args.put("reason", reason);
    }
    this.transport.delete(List.of("channels", channelId), args);
  }

  public void answer(final String channelId) throws InterruptedException {
    this.transport.post(List.of("channels", channelId, "answer"));
  }

  public void ring(final String channelId) throws InterruptedException {
    this.transport.post(List.of("channels", channelId, "ring"));
  }

  public void ringStop(final String channelId) throws InterruptedException {
    this.transport.delete(List.of("channels", channelId, "ring"), Map.of());
  }

  public void hold(final String channelId) throws InterruptedException {
    this.transport.post(List.of("channels", channelId, "hold"));
  }

  public void unhold(final String channelId) throws InterruptedException {
    this.transport.delete(List.of("channels", channelId, "hold"));
  }

  public void startMoh(final String channelId) throws InterruptedException {
    this.transport.post(List.of("channels", channelId, "moh"));
  }

  public void stopMoh(final String channelId) throws InterruptedException {
    this.transport.delete(List.of("channels", channelId, "moh"));
  }

  public void startSilence(final String channelId) throws InterruptedException {
    this.transport.post(List.of("channels", channelId, "silence"));
  }

  public void stopSilence(final String channelId) throws InterruptedException {
    this.transport.delete(List.of("channels", channelId, "silence"));
  }

  /**
   * send a play, which returns a Play ID that can be used to monitor and pause/stop it.
   */

  public void playWithId(final String channelId, final String playbackId, final String media) throws InterruptedException {

    final URI uri =
      Utils.makeUri(
        this.baseUri.toASCIIString(),
        List.of("channels", channelId, "play", playbackId),
        Map.of("media", media));

    this.transport.post(uri);

  }

  public CompletableFuture<WebSocket> events(final String appId, final Consumer<Event> handler) {

    final URI uri =
      URI.create(
        (this.baseUri.getScheme().equals("http") ? "ws"
                                                 : "wss")
          + "://"
          + this.baseUri.getAuthority()
          + this.baseUri.getPath()
          + "/events?app="
          + appId);

    LOG.info("Connecting to Websocket {}", uri);

    return this.httpClient
      .newWebSocketBuilder()
      .buildAsync(uri, new AriWebSocketListener(handler));

  }

  public static String makeBaseUrl(final HostAndPort server, final String base) {
    return String.format("http://%s/%s/ari", server.toString(), base);
  }

  public Channel getChannel(final String channelId) throws InterruptedException {
    return this.transport.get(List.of("channels", channelId), Channel.class);
  }

  public void dial(final String channelId, final String caller, final Duration timeout) throws InterruptedException {

    final URI uri =
      Utils.makeUri(
        this.baseUri.toASCIIString(),
        List.of("channels", channelId, "dial"),
        Map.of());

    this.transport.post(uri);
  }

  public String createBridge() throws InterruptedException {

    try {

      // convert to query parameters.
      final Map<String, String> args = new HashMap<>();

      final String bridgeId = UUID.randomUUID().toString();

      // mixing, holding, dtmf_events, proxy_media, video_sfu, video_single, sdp_label
      args.put("type", Set.of("mixing", "dtmf_events").stream().collect(Collectors.joining(",")));

      final URI uri = Utils.makeUri(this.baseUri, List.of("bridges", bridgeId), args);

      final HttpRequest req =
        HttpRequest.newBuilder()
          .uri(uri)
          .POST(BodyPublishers.noBody())
          .build();

      final var res = this.httpClient.send(req, BodyHandlers.ofString());

      if (res.statusCode() >= 300) {
        throw new IllegalArgumentException(res.body());
      }

      return bridgeId;

    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

  public void addChannels(final String bridgeId, final Set<String> channelIds) throws InterruptedException {

    try {

      // convert to query parameters.
      final Map<String, String> args = new HashMap<>();

      //
      args.put("channel", Set.copyOf(channelIds).stream().collect(Collectors.joining(",")));

      final URI uri = Utils.makeUri(this.baseUri, List.of("bridges", bridgeId, "addChannel"), args);

      final HttpRequest req =
        HttpRequest.newBuilder()
          .uri(uri)
          .POST(BodyPublishers.noBody())
          .build();

      final var res = this.httpClient.send(req, BodyHandlers.ofString());

      if (res.statusCode() >= 300) {
        throw new IllegalArgumentException(res.body());
      }

    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

}
