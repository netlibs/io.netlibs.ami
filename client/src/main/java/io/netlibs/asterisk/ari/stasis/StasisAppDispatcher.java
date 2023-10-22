package io.netlibs.asterisk.ari.stasis;

import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.events.ChannelEvent;
import io.netlibs.asterisk.ari.events.ChannelVarset;
import io.netlibs.asterisk.ari.events.Dial;
import io.netlibs.asterisk.ari.events.Event;
import io.netlibs.asterisk.ari.events.StasisStart;

class StasisAppDispatcher implements Consumer<Event>, StasisAppRegistration {

  private static final Logger LOG = LoggerFactory.getLogger(StasisAppDispatcher.class);

  final Map<String, ActiveStasisContext> contexts = new HashMap<>();
  private final StasisAppHandler driver;
  private final AriClient ari;
  private final CompletableFuture<WebSocket> websocket;

  private final String appId;

  StasisAppDispatcher(final AriClient ari, final String appId, final StasisAppHandler driver) {
    this.appId = appId;
    this.ari = ari;
    this.driver = driver;
    this.websocket = ari.events(appId, this);
  }

  @Override
  public CompletableFuture<WebSocket> websocket() {
    return this.websocket;
  }

  @Override
  public void accept(final Event e) {

    switch (e) {

      case final StasisStart f: {
        // start virtual thread to process this new context.
        Thread.ofVirtual().name(this.appId + ":" + f.channel().id()).start(() -> this.startStasisTask(f));
        break;
      }

      case final ChannelEvent f:
        Thread.ofVirtual().name(this.appId).start(() -> this.dispatch(this.contexts, f.channel().id(), e));
        break;

      case final ChannelVarset f:
        // dispatch
        f.channel().ifPresent(ch -> Thread.ofVirtual().name(this.appId).start(() -> this.dispatch(this.contexts, ch.id(), e)));
        break;

      case final Dial f:
        // we could optionally also pass into the orginating channel if there is one.
        // f.caller().ifPresent(ch -> this.dispatch(this.contexts, ch.id(), e));
        Thread.ofVirtual().name(this.appId).start(() -> this.dispatch(this.contexts, f.peer().id(), e));

        break;

      default:
        LOG.warn("unhandled event: {}", e.type());
        break;

    }

  }

  private void startStasisTask(final StasisStart f) {

    LOG.info(
      "{} for channel {}, args {}, state {}",
      f.type(),
      f.channel().id(),
      f.args(),
      f.channel().state());

    final ActiveStasisContext ctx = new ActiveStasisContext(Thread.currentThread(), this.ari, f);

    this.contexts.put(f.channel().id(), ctx);

    try {

      this.driver.stasisStart(ctx);

      LOG.info("Stasis app driver completed for channel {}", f.channel().id());

    }
    catch (final Exception ex) {

      ex.printStackTrace();

    }
    finally {

      this.contexts.remove(f.channel().id());

      while (!ctx.queue.isEmpty()) {
        LOG.warn("unprocessed event {}", ctx.queue.poll().type());
      }

      // we can check here to see if the channel has before removed from Stasis as we
      // will have received a StasisEnd. if we don't, then we know something is wrong.

      // we could also skip the ChannelHangupRequest.

    }

  }

  /**
   * internally dispatch an event if there is a matching context.
   */

  private void dispatch(final Map<String, ActiveStasisContext> contexts, final String ctxId, final Event e) {

    final ActiveStasisContext ctx = contexts.get(ctxId);

    if (ctx == null) {
      LOG.error("missing context for channel {}: {}", ctxId, StasisUtils.toString(e));
      return;
    }

    // LOG.debug(">>> {}", StasisUtils.toString(e));

    ctx.enqueue(e);

  }

}
