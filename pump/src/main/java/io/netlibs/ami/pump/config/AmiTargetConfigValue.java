package io.netlibs.ami.pump.config;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.net.HostAndPort;

import io.netlibs.ami.pump.model.ImmutableAmiTarget;
import io.netlibs.asterisk.ami.client.AmiCredentials;

@Value.Immutable
@JsonDeserialize(builder = ImmutableAmiTargetConfigValue.Builder.class)
public interface AmiTargetConfigValue {

  String host();

  int port();

  List<String> tls();

  String username();

  String password();

  default ImmutableAmiTarget asAmiTarget() {
    return ImmutableAmiTarget.builder()
      .target(HostAndPort.fromParts(host(), port()))
      .credentials(AmiCredentials.of(username(), password()))
      .tlsVersions(tls())
      .build();
  }

}
