package io.netlibs.asterisk.ari.events;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StasisStart(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    List<String> args,
    Channel channel,

    /** replace_channel: Channel (optional) */
    @JsonProperty("replace_channel") Optional<Channel> replaceChannel

) implements Event, ChannelEvent {

}
