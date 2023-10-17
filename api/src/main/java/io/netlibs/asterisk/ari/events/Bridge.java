package io.netlibs.asterisk.ari.events;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Style(jacksonIntegration = true)
@JsonDeserialize(builder = ImmutableBridge.Builder.class)
@Value.Immutable
public interface Bridge {

  /** bridge_class: string - Bridging class */
  @JsonProperty("bridge_class")
  String bridgeClass();

  /** bridge_type: string - Type of bridge technology */
  @JsonProperty("bridge_type")
  String bridgeType();

  /** channels: List[string] - Ids of channels participating in this bridge */
  @JsonProperty
  List<String> channels();

  /** creationtime: Date - Timestamp when bridge was created */
  @JsonProperty("creationtime")
  Instant creationTime();

  /** creator: string - Entity that created the bridge */
  @JsonProperty
  String creator();

  /** id: string - Unique identifier for this bridge */
  @JsonProperty
  String id();

  /** name: string - Name the creator gave the bridge */
  @JsonProperty
  String name();

  /** technology: string - Name of the current bridging technology */
  @JsonProperty
  String technology();

  /**
   * video_mode: string (optional) - The video mode the bridge is using. One of 'none', 'talker',
   * 'sfu', or 'single'.
   */
  @JsonProperty("video_mode")
  Optional<String> videoMode();

  /**
   * video_source_id: string (optional) - The ID of the channel that is the source of video in this
   * bridge, if one exists.
   */

  @JsonProperty("video_source_id")
  Optional<String> videoSourceId();

}
