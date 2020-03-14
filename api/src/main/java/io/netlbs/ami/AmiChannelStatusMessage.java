package io.netlbs.ami;

public interface AmiChannelStatusMessage extends AmiMessage {

  public static class Connected implements AmiChannelStatusMessage {

    private final AmiVersion version;

    public Connected(AmiVersion version) {
      this.version = version;
    }

  }

  static AmiMessage connected(AmiVersion version) {
    return new Connected(version);
  }

}
