package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StasisEnd(

    @JsonProperty("asterisk_id") String asteriskId,

    String type,

    /** application: string - Name of the application receiving the event. */
    String application,

    /** timestamp: Date - Time at which this event was created. */
    String timestamp,

    /** channel: Channel */
    Channel channel

) implements Event, ChannelEvent {

}
