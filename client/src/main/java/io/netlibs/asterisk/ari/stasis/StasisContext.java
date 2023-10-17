package io.netlibs.asterisk.ari.stasis;

import java.util.List;

import io.netlibs.asterisk.ari.events.Channel;
import io.netlibs.asterisk.ari.events.Event;
import io.netlibs.asterisk.ari.events.StasisStart;

public interface StasisContext extends ChannelContext {

  record StartParams(String application, List<String> arguments) {
  }

  /**
   * the starting parameters received in the {@link StasisStart}.
   */

  StartParams startParams();

  /**
   * the channel ID for this stasis context.
   */

  String channelId();

  /**
   * most recent snapshot of the channel state
   */

  Channel snapshot();

  /**
   * read the next event, blocking until available or null if the session has ended.
   */

  Event read() throws InterruptedException;

}
