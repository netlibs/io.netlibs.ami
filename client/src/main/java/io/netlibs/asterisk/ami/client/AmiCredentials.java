package io.netlibs.asterisk.ami.client;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(redactedMask = "********")
public interface AmiCredentials {

  @Value.Parameter(order = 0)
  String username();

  @Value.Redacted
  @Value.Parameter(order = 1)
  String secret();

  static ImmutableAmiCredentials of(final String username, final String secret) {
    return ImmutableAmiCredentials.of(username, secret);
  }

}
