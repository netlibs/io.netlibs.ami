package io.netlibs.asterisk.ari.events;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Q931CauseCode implements HangupCause {

  NOT_DEFINED(0),

  UNALLOCATED(1),
  NO_ROUTE_DESTINATION(3),

  NORMAL(16),
  USER_BUSY(17),

  /** No user responding */
  NO_USER_RESPONSE(18),

  /** No answer from user (user alerted) */
  NO_ANSWER(19),

  SUBSCRIBER_ABSENT(20),

  CALL_REJECTED(21),
  NUMBER_CHANGED(22),

  DESTINATION_OUT_OF_ORDER(27),

  INVALID_NUMBER_FORMAT(28),

  FACILITY_REJECTED(29),

  NETWORK_OUT_OF_ORDER(38),

  NORMAL_TEMPORARY_FAILURE(41),

  SWITCH_CONGESTION(42),

  BEARERCAPABILITY_NOTAVAIL(58),

  RECOVERY_ON_TIMER_EXPIRE(102),

  INTERWORKING(127),

  ;

  private static final Map<Integer, Q931CauseCode> codes = Arrays.stream(values()).collect(Collectors.toMap(Q931CauseCode::code, Function.identity()));

  private int code;

  Q931CauseCode(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }

  public static Optional<Q931CauseCode> of(int code) {
    return Optional.ofNullable(codes.get(code));
  }

}
