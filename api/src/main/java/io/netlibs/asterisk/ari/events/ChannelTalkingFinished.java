package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelTalkingFinished(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    Channel channel,
    @JsonProperty("duration") int durationMillis

) implements Event, ChannelEvent {

}
