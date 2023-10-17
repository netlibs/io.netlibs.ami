package io.netlibs.ami.pump.model;

import java.util.Set;

import org.immutables.value.Value;

import com.google.common.net.HostAndPort;

import io.netlibs.asterisk.ami.client.AmiCredentials;

@Value.Immutable
public interface AmiTarget {

  HostAndPort target();

  AmiCredentials credentials();

  Set<String> tlsVersions();

}
