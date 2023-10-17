package io.netlibs.asterisk.ari.stasis;

import io.netlibs.asterisk.ari.events.ChannelEvent;
import io.netlibs.asterisk.ari.events.ChannelHangupRequest;
import io.netlibs.asterisk.ari.events.ChannelVarset;
import io.netlibs.asterisk.ari.events.Dial;
import io.netlibs.asterisk.ari.events.Event;

public final class StasisUtils {

  public static String toString(final Event e) {
    return switch (e) {

      case final Dial dial -> String.format("%s %s", e.type(), dial.dialstatus());

      case final ChannelHangupRequest ce -> String
        .format(
          "%s %s state %s cause %s vars %s",
          e.type(),
          ce.channel().id(),
          ce.channel().state(),
          ce.cause(),
          ce.channel().channelVars());

      case final ChannelEvent ce -> String.format("%s %s vars %s", e.type(), ce.channel().state(), ce.channel().channelVars());
      case final ChannelVarset varset -> String.format("%s %s = %s", e.type(), varset.variable(), varset.value());
      default -> String.format("%s", e.type());
    };
  }

}
