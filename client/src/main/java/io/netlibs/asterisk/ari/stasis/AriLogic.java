package io.netlibs.asterisk.ari.stasis;

import java.util.Map;
import java.util.Set;

import io.netlibs.asterisk.ari.events.Channel;

final class AriLogic {

  private static final Map<Channel.State, Set<Channel.State>> allowedTransitions =
    Map.ofEntries(

      Map.entry(Channel.State.RING, Set.of(Channel.State.UP))

    );

  /**
   * is it possible to transition from one state to another?
   */

  static boolean isPossibleTransition(final Channel.State currentState, final Channel.State desiredState) {
    return allowedTransitions.getOrDefault(currentState, Set.of()).contains(desiredState);
  }

}
