package io.netlibs.ami.client;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(redactedMask = "********")
public interface AmiCredentials {

  @Value.Parameter(order = 0)
  String username();

  @Value.Redacted
  @Value.Parameter(order = 1)
  String secret();

  static ImmutableAmiCredentials of(String username, String secret) {
    return ImmutableAmiCredentials.of(username, secret);
  }

}
