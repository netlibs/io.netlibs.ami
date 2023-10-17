package io.netlibs.asterisk.ari.client;

public interface Ari {

  void hangup(String channelId, String reason) throws InterruptedException;

  default void hangup(final String channelId) throws InterruptedException {
    this.hangup(channelId, null);
  }

}
