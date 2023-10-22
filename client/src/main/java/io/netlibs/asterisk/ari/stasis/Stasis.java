package io.netlibs.asterisk.ari.stasis;

import java.util.concurrent.StructuredTaskScope;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.commands.ChannelEndpoint;
import io.netlibs.asterisk.ari.commands.OriginateParams;
import io.netlibs.asterisk.ari.events.Bridge;
import io.netlibs.asterisk.ari.events.Channel;

public final class Stasis {

  private Stasis() {
  }

  /**
   * waits until the channel is in an answered state, failing if interrupted or the channel reaches a state where it's not possible to be
   * answered.
   *
   * for outgoing calls, this will wait until the call is answered or an error occurs.
   *
   * for incoming calls, this will issue an answer ARI call.
   *
   * using a single method for both incoming and outgoing simplifies logic of stasis applications.
   *
   * @throw {@link IllegalStateException} if the channel is in a state which can not reach answered.
   *
   */

  public static void answer(final StasisContext ctx) throws InterruptedException {

    switch (ctx.snapshot().state()) {
      case RING:
        ctx.answer();
        return;
      case BUSY:
      case DIALING:
      case DIALING_OFFHOOK:
      case DOWN:
      case MUTE:
      case OFFHOOK:
      case PRERING:
      case RESERVED:
      case RINGING:
      case UNKNOWN:
      case UP:
      default:
        throw new IllegalStateException(ctx.snapshot().state().toString());
    }

  }

  /**
   * similar(ish) to the Dial() dialplan app. This creates a channel and bridge, adds the calling channel and the new channel in, and then
   * calls dial on the outgoing channel.
   *
   * the invocation will return once either leg has terminated, and resources have been cleaned up.
   *
   * @throws InterruptedException
   *
   */

  public static void dial(final StasisContext ctx, final ChannelEndpoint target) throws InterruptedException {

    final AriClient ari = ctx.ari();

    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

      // 1A: create the outgoing channel
      final Channel dialedChannel =
        ari.createChannel(
          OriginateParams.builder()
            .endpoint(target)
            .app(ctx.startParams().application())
            .appArgs("-")
            .build());

      // 1B: create the bridge
      final Bridge bridge = ari.createBridge();

      // 2A: add the outgoing channel to the bridge
      // 2B: add the incoming channel to the bridge
      ari.addChannels(bridge.id(), dialedChannel.id(), ctx.channelId());

      // 3: call dial on the outgoing channel.
      ari.dial(dialedChannel.id());

      // with one thread, we'll wait for the outgoing channel to complete.
      // and with the other, wait for the calling channel to complete.

    }

  }

}
