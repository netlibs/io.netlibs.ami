package io.netlibs.asterisk.ari.commands;

import java.time.Duration;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

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
abstract class AbstractDialParams {

  public abstract String channelId();

  public abstract String callerId();

  public abstract Duration timeout();

}
