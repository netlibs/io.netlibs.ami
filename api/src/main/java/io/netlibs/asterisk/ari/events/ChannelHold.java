package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelHold(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    Channel channel,

    @JsonProperty Optional<String> musicclass

) implements Event, ChannelEvent {

}
