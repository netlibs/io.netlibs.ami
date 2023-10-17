package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelDtmfReceived(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    /** channel: Channel */
    Channel channel,

    String digit,

    @JsonProperty("duration_ms") int durationMs

) implements Event, ChannelEvent {

}

