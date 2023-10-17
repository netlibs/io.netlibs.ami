package io.netlibs.asterisk.ari.stasis;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

import io.netlibs.asterisk.ari.client.AriClient;

public interface StasisAppRegistration {

  static StasisAppRegistration register(final AriClient ari, final String appId, final StasisAppHandler handler) throws InterruptedException {
    return new StasisAppDispatcher(ari, appId, handler);
  }

  CompletableFuture<WebSocket> websocket();

}
