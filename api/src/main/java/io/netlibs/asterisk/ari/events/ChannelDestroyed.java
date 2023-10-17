package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelDestroyed(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    HangupCause cause,

    /** cause_txt: string - Text representation of the cause of the hangup */
    @JsonProperty("cause_txt") String causeTxt,

    Channel channel

) implements Event, ChannelEvent {

}
