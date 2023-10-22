package io.netlibs.asterisk.ari.stasis;

import java.time.Duration;

public record DtmfEvent(DtmfDigit digit, Duration duration) {

}
