package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * weirdly, asterisk sends variable changes using the channel one for global too. so we're stuck
 * with this not being a channel event.
 */

public record ChannelVarset(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    Optional<Channel> channel,

    String variable,
    String value

) implements Event {

}
