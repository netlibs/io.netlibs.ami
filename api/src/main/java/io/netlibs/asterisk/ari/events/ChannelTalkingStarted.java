package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelTalkingStarted(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    Channel channel

) implements Event, ChannelEvent {

}

