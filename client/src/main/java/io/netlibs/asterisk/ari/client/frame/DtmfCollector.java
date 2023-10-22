package io.netlibs.asterisk.ari.client.frame;

import java.util.Optional;

/**
 * collect a set of DTMF events, reducing to a single value to return when ready.
 */

@FunctionalInterface
public interface DtmfCollector<R> {

  /**
   * called when there is a DTMF Event with the current state of the buffer.
   *
   * the handler will return an empty optional if it wants more data, an optional with value that will be returned if complete, or an
   * exception if an error encountered.
   *
   */

  Optional<R> accumulate(String buffer);

}
