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
abstract class AbstractExternalMediaParams {

  // channelId: string - The unique id to assign the channel on creation.
  @JsonProperty
  public abstract String channelId();

  // app: string - (required) Stasis Application to place channel into
  @JsonProperty
  public abstract String app();

  // external_host: string - (required) Hostname/ip:port of external host
  @JsonProperty("external_host")
  public abstract String externalHost();

  // encapsulation: string - Payload encapsulation protocol
  // Default: rtp
  // Allowed values: rtp, audiosocket
  @JsonProperty
  public abstract Optional<String> encapsulation();

  // transport: string - Transport protocol
  // Default: udp
  // Allowed values: udp, tcp
  @JsonProperty
  public abstract Optional<String> transport();

  // format: string - (required) Format to encode audio in
  @JsonProperty
  public abstract String format();

  // data: string - An arbitrary data field
  @JsonProperty
  public abstract Optional<String> data();

  // variables: containers - The "variables" key in the body object holds variable key/value pairs to set on the channel on creation.
  @JsonProperty
  public abstract Map<String, String> variables();

}
