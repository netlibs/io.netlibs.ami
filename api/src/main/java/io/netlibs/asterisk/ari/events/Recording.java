package io.netlibs.asterisk.ari.events;

import java.util.Optional;
import java.util.OptionalInt;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;

@Value.Immutable
public interface Recording {

  // cause: string (optional) - Cause for recording failure if failed
  Optional<String> cause();

  // duration: int (optional) - Duration in seconds of the recording
  OptionalInt duration();

  // name: string - Base name for the recording
  String name();

  // format: string - Recording format (wav, gsm, etc.)
  String format();

  // state: string
  String state();

  // silence_duration: int (optional) - Duration of silence, in seconds, detected in the recording.
  // This is only available if the recording was initiated with a non-zero maxSilenceSeconds.
  // state: string
  @JsonProperty("silence_duration")
  OptionalInt silenceDuration();

  // talking_duration: int (optional) - Duration of talking, in seconds, detected in the recording.
  // This is only available if the recording was initiated with a non-zero maxSilenceSeconds.
  @JsonProperty("talking_duration")
  OptionalInt talkingDuration();

  // target_uri: string - URI for the channel or bridge being recorded
  @JsonProperty("target_uri")
  String targetUri();

}
