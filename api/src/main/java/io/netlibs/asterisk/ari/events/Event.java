package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "type", visible = true, defaultImpl = JsonTypeInfo.class)
@JsonSubTypes({

  @Type(value = StasisStart.class, name = "StasisStart"),
  @Type(value = StasisEnd.class, name = "StasisEnd"),

  @Type(value = Dial.class, name = "Dial"),

  @Type(value = ChannelStateChange.class, name = "ChannelStateChange"),
  @Type(value = ChannelConnectedLine.class, name = "ChannelConnectedLine"),

  @Type(value = ChannelHold.class, name = "ChannelHold"),
  @Type(value = ChannelUnhold.class, name = "ChannelUnhold"),
  @Type(value = ChannelDtmfReceived.class, name = "ChannelDtmfReceived"),
  @Type(value = ChannelVarset.class, name = "ChannelVarset"),
  @Type(value = ChannelDialplan.class, name = "ChannelDialplan"),
  @Type(value = ChannelCallerId.class, name = "ChannelCallerId"),

  @Type(value = ChannelTalkingStarted.class, name = "ChannelTalkingStarted"),
  @Type(value = ChannelTalkingFinished.class, name = "ChannelTalkingFinished"),

  @Type(value = ChannelEnteredBridge.class, name = "ChannelEnteredBridge"),
  @Type(value = ChannelLeftBridge.class, name = "ChannelLeftBridge"),

  @Type(value = ChannelHangupRequest.class, name = "ChannelHangupRequest"),
  @Type(value = ChannelDestroyed.class, name = "ChannelDestroyed"),

  @Type(value = PlaybackStarted.class, name = "PlaybackStarted"),
  @Type(value = PlaybackContinuing.class, name = "PlaybackContinuing"),
  @Type(value = PlaybackFinished.class, name = "PlaybackFinished"),

  @Type(value = RecordingStarted.class, name = "RecordingStarted"),
  @Type(value = RecordingFailed.class, name = "RecordingFailed"),
  @Type(value = RecordingFinished.class, name = "RecordingFinished"),

})
public sealed interface Event permits
    //
    Dial,
    //
    StasisStart, StasisEnd,
    //
    PlaybackStarted, PlaybackContinuing, PlaybackFinished,
    //
    RecordingStarted, RecordingFailed, RecordingFinished,
    //
    ChannelStateChange, ChannelDtmfReceived, ChannelHold, ChannelUnhold, ChannelHangupRequest,
    ChannelVarset, ChannelCallerId, ChannelTalkingStarted, ChannelTalkingFinished, ChannelDialplan, ChannelDestroyed,
    ChannelEnteredBridge, ChannelLeftBridge
    //
    , ChannelConnectedLine {

  @JsonProperty("asterisk_id")
  String asteriskId();

  @JsonProperty
  String type();

  /** application: string - Name of the application receiving the event. */
  @JsonProperty
  String application();

  /** timestamp: Date - Time at which this event was created. */
  @JsonProperty
  String timestamp();

}
