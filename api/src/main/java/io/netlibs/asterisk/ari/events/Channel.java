package io.netlibs.asterisk.ari.events;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Style(jacksonIntegration = true)
@JsonDeserialize(builder = ImmutableChannel.Builder.class)
@Value.Immutable
public interface Channel {

  enum State {

    /** Channel is down and available */
    @JsonProperty("Down")
    DOWN,

    /** Channel is down, but reserved */
    @JsonProperty("Rsrvd")
    RESERVED,

    /** Channel is off hook */
    @JsonProperty("OffHook")
    OFFHOOK,

    /** Digits (or equivalent) have been dialed */
    @JsonProperty("Dialing")
    DIALING,

    /** Line is ringing */
    @JsonProperty("Ring")
    RING,

    /** Remote end is ringing */
    @JsonProperty("Ringing")
    RINGING,

    /** Line is up */
    @JsonProperty("Up")
    UP,

    /** Line is busy */
    @JsonProperty("Busy")
    BUSY,

    /** Digits (or equivalent) have been dialed while offhook */
    @JsonProperty("Dialing Offhook")
    DIALING_OFFHOOK,

    /** Channel has detected an incoming call and is waiting for ring */
    @JsonProperty("Pre-ring")
    PRERING,

    /** Do not transmit voice data */
    @JsonProperty("Mute")
    MUTE,

    @JsonEnumDefaultValue
    @JsonProperty("Unknown")
    UNKNOWN,

  }

  @JsonProperty
  String id();

  @JsonProperty
  String name();

  @JsonProperty
  State state();

  @JsonProperty
  CallerID caller();

  @JsonProperty
  CallerID connected();

  @JsonProperty
  String accountcode();

  @JsonProperty
  DialplanCEP dialplan();

  @JsonProperty
  Instant creationtime();

  @JsonProperty("protocol_id")
  Optional<String> protocolId();

  @JsonProperty
  String language();

  @JsonProperty("channelvars")
  Map<String, String> channelVars();

}
