package io.netlibs.ami.pump.event;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.netlibs.ami.pump.model.SourceInfo;

@Value.Immutable
public interface KinesisEvent {

  SourceInfo source();

  ObjectNode payload();

}
