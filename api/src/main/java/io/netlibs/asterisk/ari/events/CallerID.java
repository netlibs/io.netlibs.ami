package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Style(jacksonIntegration = true)
@JsonDeserialize(builder = ImmutableCallerID.Builder.class)
@Value.Immutable
public interface CallerID {

  Optional<String> name();

  Optional<String> number();

}
