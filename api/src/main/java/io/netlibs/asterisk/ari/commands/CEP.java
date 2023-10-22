package io.netlibs.asterisk.ari.commands;

import java.util.OptionalInt;

public record CEP(String context, String exten, OptionalInt priority) {

  public static CEP of(final String context, final String exten) {
    return new CEP(context, exten, OptionalInt.empty());
  }

  public static CEP of(final String context, final String exten, final int priority) {
    return new CEP(context, exten, OptionalInt.of(priority));
  }

}
