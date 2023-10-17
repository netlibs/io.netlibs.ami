package io.netlibs.asterisk.ari.events;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Style(jacksonIntegration = true)
@JsonDeserialize(builder = ImmutableDialplanCEP.Builder.class)
@Value.Immutable
public interface DialplanCEP {

  @JsonProperty
  String context();

  @JsonProperty
  String exten();

  @JsonProperty
  String priority();

  @JsonProperty
  String app_name();

  @JsonProperty
  String app_data();

}
