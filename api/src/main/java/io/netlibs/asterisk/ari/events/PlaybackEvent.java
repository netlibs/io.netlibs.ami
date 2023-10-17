package io.netlibs.asterisk.ari.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public sealed interface PlaybackEvent permits PlaybackStarted, PlaybackContinuing, PlaybackFinished {

  @JsonProperty
  Playback playback();

}
