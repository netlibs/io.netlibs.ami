package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlaybackContinuing(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    Playback playback

) implements PlaybackEvent, Event {

}
