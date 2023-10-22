package io.netlibs.asterisk.ari.client.http;

import java.net.http.WebSocket;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.netlibs.asterisk.ari.events.Event;

public class AriWebSocketListener implements WebSocket.Listener {

  private static final Logger LOG = LoggerFactory.getLogger(AriWebSocketListener.class);

  private static ObjectMapper mapper = new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());

  private final StringBuilder buffer = new StringBuilder();

  private final Consumer<Event> handler;

  public AriWebSocketListener(final Consumer<Event> handler) {
    this.handler = Objects.requireNonNull(handler);
  }

  @Override
  public void onOpen(final WebSocket webSocket) {
    LOG.info("connected to ARI websocket");
    webSocket.request(1);
  }

  @Override
  public CompletionStage<?> onText(final WebSocket webSocket, final CharSequence data, final boolean last) {

    try {

      this.buffer.append(data);

      if (last) {

        final String input = this.buffer.toString();
        this.buffer.setLength(0);
        try {
          this.handler.accept(mapper.readValue(input, Event.class));

        }
        catch (final Exception ex) {
          LOG.warn("exception while processing ARI event: {}", ex.getMessage(), ex);

          System.err.println();
          System.err.println(input);
          System.err.println();

        }
      }

    }
    finally {
      webSocket.request(1);
    }
    return null;
  }

  @Override
  public CompletionStage<?> onClose(final WebSocket webSocket, final int statusCode, final String reason) {
    LOG.warn("ARI websocket closed: {} {}", statusCode, reason);
    return null;
  }

  @Override
  public void onError(final WebSocket webSocket, final Throwable error) {
    LOG.error("AIR websocket error: {}", error.getMessage(), error);
  }

}
