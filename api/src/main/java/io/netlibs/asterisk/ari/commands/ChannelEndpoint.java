package io.netlibs.asterisk.ari.commands;

import com.fasterxml.jackson.annotation.JsonValue;

public interface ChannelEndpoint {

  @Override
  @JsonValue
  String toString();

}
