package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;

@Value.Immutable
public interface Playback {

  /** id: string - ID for this playback operation */
  String id();

  /**
   * language: string (optional) - For media types that support multiple languages, the language
   * requested for playback.
   */

  Optional<String> language();

  /** media_uri: string - The URI for the media currently being played back. */
  @JsonProperty("media_uri")
  String mediaUri();

  /**
   * next_media_uri: string (optional) - If a list of URIs is being played, the next media URI to be
   * played back.
   */

  @JsonProperty("next_media_uri")
  Optional<String> nextMediaUri();

  /** state: string - Current state of the playback operation. */
  String state();

  /** target_uri: string - URI for the channel or bridge to play the media on */
  @JsonProperty("target_uri")
  String targetUri();

}
