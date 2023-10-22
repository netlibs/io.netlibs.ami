package io.netlibs.asterisk.ari.commands;

public record StasisInvokeParams(String appName, String args) {

  public static StasisInvokeParams of(final String app, final String args) {
    return new StasisInvokeParams(app, args);
  }

}
