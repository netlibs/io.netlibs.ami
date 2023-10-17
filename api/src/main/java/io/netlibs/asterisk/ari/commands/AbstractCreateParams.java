package io.netlibs.asterisk.ari.commands;

import java.util.Map;
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
abstract class AbstractCreateParams {

  @JsonProperty
  public abstract String endpoint();

  @JsonProperty
  public abstract String app();

  @JsonProperty
  public abstract Optional<String> appArgs();

  @JsonProperty
  public abstract Optional<String> channelId();

  @JsonProperty
  public abstract Optional<String> otherChannelId();

  @JsonProperty
  public abstract Optional<String> originator();

  @JsonProperty
  public abstract Optional<String> formats();

  @JsonProperty
  public abstract Map<String, String> variables();

}
