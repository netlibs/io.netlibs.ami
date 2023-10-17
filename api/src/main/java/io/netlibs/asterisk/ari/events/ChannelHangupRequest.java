package io.netlibs.asterisk.ari.events;

import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

public record ChannelHangupRequest(

    @JsonProperty("asterisk_id") String asteriskId,

    @JsonProperty String type,

    /** application: string - Name of the application receiving the event. */
    @JsonProperty String application,

    /** timestamp: Date - Time at which this event was created. */
    @JsonProperty String timestamp,

    /** cause: int (optional) - Integer representation of the cause of the hangup. */
    OptionalInt cause,

    /** channel: Channel */
    @JsonProperty Channel channel,

    /** soft: boolean (optional) - Whether the hangup request was a soft hangup request. */
    @Nullable Boolean soft

) implements Event, ChannelEvent {

}
