package io.netlibs.asterisk.ari.commands;

import java.util.Optional;
import java.util.OptionalInt;

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
abstract class AbstractRecordParams {

  @JsonProperty
  public abstract String name();

  @JsonProperty
  public abstract String format();

  @JsonProperty
  public abstract OptionalInt maxDurationSeconds();

  @JsonProperty
  public abstract OptionalInt maxSilenceSeconds();

  @JsonProperty
  public abstract Optional<String> ifExists(); // fail, overwrite, append

  @JsonProperty
  @Value.Default
  public boolean beep() {
    return false;
  }

  @JsonProperty
  public abstract Optional<String> terminateOn();

}
