package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelLeftBridge(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    /** channel: Channel */
    Bridge bridge,

    /** channel: Channel */
    Channel channel

) implements Event, ChannelEvent {

}
