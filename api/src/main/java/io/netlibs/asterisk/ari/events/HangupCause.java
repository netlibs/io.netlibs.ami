package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public interface HangupCause {

  @JsonCreator
  public static HangupCause fromCode(int code) {
    return Q931CauseCode.of(code).orElse(Q931CauseCode.NOT_DEFINED);
  }

  @JsonValue
  public int code();

}
