package io.netlibs.asterisk.ari.client.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.commands.ChannelEndpoint;
import io.netlibs.asterisk.ari.commands.DialParams;
import io.netlibs.asterisk.ari.commands.ExternalMediaParams;
import io.netlibs.asterisk.ari.commands.OriginateParams;
import io.netlibs.asterisk.ari.commands.RecordParams;
import io.netlibs.asterisk.ari.commands.SnoopChannelParams;
import io.netlibs.asterisk.ari.events.Bridge;
import io.netlibs.asterisk.ari.events.Channel;
import io.netlibs.asterisk.ari.events.Event;

public final class HttpAriClient implements AriClient {

  private static final String CHANNELS = "channels";
  private static final String PLAYBACKS = "playbacks";
  private static final String LIVE = "live";
  private static final String RECORDINGS = "recordings";

  private static final Logger LOG = LoggerFactory.getLogger(HttpAriClient.class);
  private static final ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

  private final HttpClient httpClient;
  private final URI baseUri;

  public HttpAriClient(final String baseUri, final HttpClient httpClient) {
    this.baseUri = URI.create(baseUri.replaceFirst("/*$", ""));
    this.httpClient = httpClient;
  }

  public static String makeBaseUrl(final HostAndPort server, final String base) {
    return String.format("http://%s/%s/ari", server.toString(), base);
  }

  private void send(final HttpRequest req) throws InterruptedException {
    try {

      final HttpResponse<Void> res = this.httpClient.send(req, BodyHandlers.discarding());

      LOG.debug("ARI response to {}: response {}", req.uri(), res.statusCode());

      if (res.statusCode() != 200) {
        throw new AriException(String.format("Unecpected HTTP status code %d", res.statusCode()), req, res);
      }

    }
    catch (final IOException ex) {
      throw new AriException(ex, req);
    }

  }

  private <T> T send(final HttpRequest req, final BodyHandler<T> bodyReader) throws InterruptedException {
    try {

      final HttpResponse<T> res = this.httpClient.send(req, bodyReader);

      LOG.debug("ARI response to {}: response {}", req.uri(), res.statusCode());

      if (res.statusCode() != 200) {
        throw new AriException(String.format("Unecpected HTTP status code %d", res.statusCode()), req, res);
      }

      return res.body();

    }
    catch (final IOException ex) {
      throw new AriException(ex, req);
    }
  }

  @Override
  public Channel createChannel(final OriginateParams params) throws InterruptedException {

    // convert to query parameters.
    final Map<String, String> args =
      mapper.convertValue(
        params,
        TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class));

    final URI uri = Utils.makeUri(this.baseUri, List.of(CHANNELS, "create"), args);

    record Payload(Map<String, String> variables) {
    }

    final HttpRequest req =
      HttpRequest.newBuilder()
        .uri(uri)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
        .POST(HttpUtils.jsonPublisher(new Payload(params.variables())))
        .build();

    return this.send(req, HttpUtils.bodyReader(Channel.class));

  }

  @Override
  public void hangup(final String channelId, final String reason) throws InterruptedException {

    final HashMap<String, String> queryParams = new HashMap<>();

    if (reason != null) {
      queryParams.put("reason", reason);
    }

    final HttpRequest request =
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId), queryParams))
        .DELETE()
        .build();

    this.send(request);

  }

  @Override
  public void answer(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "answer")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void ring(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "ring")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void ringStop(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "ring")))
        .DELETE()
        .build());
  }

  @Override
  public void hold(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "hold")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void unhold(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "hold")))
        .DELETE()
        .build());
  }

  @Override
  public void startMoh(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "moh")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void stopMoh(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "moh")))
        .DELETE()
        .build());
  }

  @Override
  public void startSilence(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "silence")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void stopSilence(final String channelId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "silence")))
        .DELETE()
        .build());
  }

  @Override
  public void sendDTMF(final String channelId, final String dtmf) throws InterruptedException {
    final Map<String, String> params = new HashMap<>();
    params.put("dtmf", dtmf);
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "dtmf"), params))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public String getChannelVar(final String channelId, final String variable) throws InterruptedException {
    record VariableResponse(String variable) {
    }
    return this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "variable"), Map.of("variable", variable)))
        .GET()
        .build(),
      HttpUtils.bodyReader(VariableResponse.class))
      .variable();
  }

  @Override
  public void setChannelVar(final String channelId, final String variable, final String value) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "variable"), Map.of("variable", variable, "value", value)))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public Channel snoopChannelWithId(final String channelId, final SnoopChannelParams params) throws InterruptedException {
    // convert to query parameters.
    final Map<String, String> args = HttpUtils.makeArgs(params);
    return this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "snoop"), args))
        .POST(BodyPublishers.noBody())
        .build(),
      HttpUtils.bodyReader(Channel.class));
  }

  @Override
  public Channel externalMedia(final String channelId, final ExternalMediaParams params) throws InterruptedException {

    // convert to query parameters.
    final Map<String, String> args = Maps.filterKeys(HttpUtils.makeArgs(params), k -> !"variables".equals(k));

    record Payload(Map<String, String> variables) {
    }

    return this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "snoop"), args))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
        .POST(HttpUtils.jsonPublisher(new Payload(params.variables())))
        .build(),
      HttpUtils.bodyReader(Channel.class));

  }

  @Override
  public void startRecording(final String channelId, final RecordParams params) throws InterruptedException {
    final Map<String, String> args = HttpUtils.makeArgs(params);
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "record"), args))
        .header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
        .POST(BodyPublishers.noBody())
        .build());
  }

  public Channel channel(final String channelId) throws InterruptedException {
    return this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId)))
        .GET()
        .build(),
      HttpUtils.bodyReader(Channel.class));
  }

  @Override
  public void dial(final DialParams params) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, params.channelId(), "dial")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void redirect(final String channelId, final ChannelEndpoint endpoint) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(CHANNELS, channelId, "redirect"), Map.of("endpoint", endpoint.toString())))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public Bridge createBridge() throws InterruptedException {

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

      final var res = this.httpClient.send(req, HttpUtils.bodyReader(Bridge.class));

      if (res.statusCode() >= 300) {
        throw new IllegalStateException("invalid state for bridge creation");
      }

      return res.body();

    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  @Override
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
      throw new UncheckedIOException(e);
    }

  }

  @Override
  public CompletableFuture<WebSocket> events(final String appId, final Consumer<Event> handler) {

    final URI uri =
      URI.create(
        ("http".equals(this.baseUri.getScheme()) ? "ws"
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

  /**
   * send a play, which returns a Play ID that can be used to monitor and pause/stop it.
   */

  @Override
  public void playWithId(final String channelId, final String playbackId, final String media) throws InterruptedException {

    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri,
          List.of(
            CHANNELS,
            channelId,
            "play",
            playbackId),
          Map.of("media", media)))
        .POST(BodyPublishers.noBody())
        .build());

  }

  @Override
  public void controlPlayback(final String playbackId, final PlaybackOperation op) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(PLAYBACKS, playbackId), Map.of("operation", op.toString().toLowerCase())))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void stopPlayback(final String playbackId) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(PLAYBACKS, playbackId)))
        .DELETE()
        .build());
  }

  @Override
  public void controlRecording(final String recordingName, final RecordingOperation operation) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(RECORDINGS, LIVE, recordingName, operation.toString().toLowerCase())))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void stopRecording(final String recordingName) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(RECORDINGS, LIVE, recordingName, "stop")))
        .POST(BodyPublishers.noBody())
        .build());
  }

  @Override
  public void cancelRecording(final String recordingName) throws InterruptedException {
    this.send(
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, List.of(RECORDINGS, LIVE, recordingName)))
        .DELETE()
        .build());
  }

}
