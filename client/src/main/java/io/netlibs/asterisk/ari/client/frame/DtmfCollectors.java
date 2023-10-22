package io.netlibs.asterisk.ari.client.frame;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Preconditions;

import io.netlibs.asterisk.ari.stasis.DtmfDigit;

public final class DtmfCollectors {

  private DtmfCollectors() {
  }

  /**
   * a collector which matches a fixed length set of characters, stopping early when it receives any of the provided stop chars. The stop
   * character will be included in the returned value.
   */

  public static DtmfCollector<String> fixedLength(final int length, final DtmfDigit... stop) {
    return fixedLength(length, Set.of(stop));
  }

  /**
   * a collector which matches a fixed length set of characters, stopping early when it receives any of the provided stop chars. The stop
   * character will be included in the returned value.
   */

  public static DtmfCollector<String> fixedLength(final int length, final Collection<DtmfDigit> stop) {
    return new FixedLength(length, Set.copyOf(stop));
  }

  /*
   * a fixed length collector.
   */

  private static class FixedLength implements DtmfCollector<String> {

    private final int length;
    private final Set<DtmfDigit> stop;

    private FixedLength(final int length, final Set<DtmfDigit> stop) {
      Preconditions.checkArgument(length > 0, "Fixed DTMF collection length must be > 0");
      this.length = length;
      this.stop =
        stop.isEmpty() ? Set.of()
                       : EnumSet.copyOf(stop);
    }

    @Override
    public Optional<String> accumulate(final String buffer) {

      for (int i = 0; i < buffer.length(); ++i) {

        final DtmfDigit digit = DtmfDigit.of(buffer.charAt(i));

        if (this.stop.contains(digit) || ((i + 1) == this.length)) {
          return Optional.of(buffer.substring(0, i + 1));
        }

      }

      return Optional.empty();

    }

  }

}
