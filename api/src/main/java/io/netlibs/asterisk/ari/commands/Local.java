package io.netlibs.asterisk.ari.commands;

public record Local(String extension, String context) implements ChannelEndpoint {

  public static Local of(final String extension, final String context) {
    return new Local(extension, context);
  }

  @Override
  public String toString() {
    return String.format("Local/%s@%s", this.extension(), this.context());
  }

}
