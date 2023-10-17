package io.netlibs.asterisk.ari.stasis;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.events.StasisStart;

/**
 * implements the Stasis application handler for in-memory services.
 *
 * implementation uses new fangled java virtual threads, so might churn as i iterate on best
 * practices and models - but this high level interface will remain the entry point for the socket
 * handler to dispatch to application specific logic.
 *
 * note that this design is for applications which remain active for the duration of a call, not
 * those which are stateless on the frontend. this is generally the case, as typical deployment is
 * as a sidecar to asterisk and therefore bound to the lifecycle of asterisk.
 *
 * applications which wish to perform stateless/async type handling would need to use the lower
 * level {@link AriClient}.
 *
 */

public interface StasisAppHandler {

  /**
   * called in a virtual thread when a {@link StasisStart} is received. The implementation should
   * not return until it has finished processing. events will be delivered via the context.
   */

  void stasisStart(StasisContext ctx);

}
