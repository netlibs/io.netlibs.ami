package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;


public record RecordingFailed(

    @JsonProperty("asterisk_id") String asteriskId,
    String type,
    String application,
    String timestamp,
    Recording recording

) implements Event {

}
