package io.netlibs.asterisk.ari.commands;

import java.util.Optional;

public record PJSIP(String endpointName, String uri) implements ChannelEndpoint {

  public static PJSIP of(final String endpointName) {
    return of(endpointName, null);
  }

  public static PJSIP of(final String endpointName, final String uri) {
    return new PJSIP(endpointName, uri);
  }

  @Override
  public String toString() {
    return String.format("PJSIP/%s%s", this.endpointName(), Optional.ofNullable(this.uri).map("/"::concat).orElse(""));
  }

}
