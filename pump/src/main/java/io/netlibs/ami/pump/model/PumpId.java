package io.netlibs.ami.pump.model;

import org.immutables.value.Value;

@Value.Immutable
public interface PumpId {

  @Value.Parameter(order = 0)
  String id();

  @Value.Parameter(order = 1)
  long epoch();

}
