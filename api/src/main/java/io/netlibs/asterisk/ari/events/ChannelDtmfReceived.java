package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelDtmfReceived(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    /** channel: Channel */
    Channel channel,

    // dtmf only a single char at once.
    char digit,

    @JsonProperty("duration_ms") int durationMs

) implements Event, ChannelEvent {

}
