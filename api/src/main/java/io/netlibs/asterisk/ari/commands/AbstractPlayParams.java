package io.netlibs.asterisk.ari.commands;

import java.util.List;
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
abstract class AbstractPlayParams {

  @JsonProperty
  public abstract List<String> media();

  @JsonProperty
  public abstract String lang();

  @JsonProperty
  public abstract OptionalInt offsetms();

  @JsonProperty
  public abstract OptionalInt skipms();

}
