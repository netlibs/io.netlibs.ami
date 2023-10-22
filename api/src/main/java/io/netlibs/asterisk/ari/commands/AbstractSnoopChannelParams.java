package io.netlibs.asterisk.ari.commands;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Value.Immutable
@Value.Style(
    stagedBuilder = true,
    depluralize = true,
    typeAbstract = "Abstract*",
    jakarta = true,
    jdk9Collections = true,
    typeImmutable = "*",
    visibility = Value.Style.ImplementationVisibility.PUBLIC)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
abstract class AbstractSnoopChannelParams {

  // spy: string - Direction of audio to spy on
  // Default: none
  // Allowed values: none, both, out, in
  @JsonProperty
  public abstract Optional<SpyDirection> spy();

  // whisper: string - Direction of audio to whisper into
  // Default: none
  // Allowed values: none, both, out, in
  @JsonProperty
  public abstract Optional<SpyDirection> whisper();

  // app: string - (required) Application the snooping channel is placed into
  @JsonProperty
  public abstract String app();

  // appArgs: string - The application arguments to pass to the Stasis application
  @JsonProperty
  public abstract Optional<String> appArgs();

  // snoopId: string - Unique ID to assign to snooping channel
  @JsonProperty
  public abstract Optional<String> snoopId();

}
