package io.netlibs.ami.client;

import org.immutables.value.Value;

@Value.Immutable
public interface AmiCredentials {

  @Value.Parameter(order = 0)
  String username();

  @Value.Parameter(order = 1)
  String secret();

  static ImmutableAmiCredentials of(String username, String secret) {
    return ImmutableAmiCredentials.of(username, secret);
  }

}
