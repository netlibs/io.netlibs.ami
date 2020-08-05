package io.netlibs.ami.pump.model;

import org.immutables.value.Value;

@Value.Immutable
public interface SourceInfo {

  @Value.Parameter(order = 0)
  String sourceId();

  @Value.Parameter(order = 1)
  long sourceSequence();

}
