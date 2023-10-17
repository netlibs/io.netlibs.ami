package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelCallerId(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,

    /** channel: Channel */
    Channel channel,

    /** The integer representation of the Caller Presentation value. */
    @JsonProperty("caller_presentation") int callerPresentation,

    /** - The text representation of the Caller Presentation value. */
    @JsonProperty("caller_presentation_txt") String callerPresentationText

) implements Event, ChannelEvent {


}
