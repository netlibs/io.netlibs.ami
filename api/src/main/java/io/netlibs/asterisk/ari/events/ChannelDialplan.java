package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelDialplan(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    /** channel: Channel */
    Channel channel,

    @JsonProperty("dialplan_app") String dialplanApp,

    @JsonProperty("dialplan_app_data") Optional<String> dialplanAppData

) implements Event, ChannelEvent {

}

