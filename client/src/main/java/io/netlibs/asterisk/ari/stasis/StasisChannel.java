package io.netlibs.asterisk.ari.stasis;

import java.util.Iterator;
import java.util.Objects;

import io.netlibs.asterisk.ari.client.frame.DtmfCollector;
import io.netlibs.asterisk.ari.client.frame.LogicalChannel;
import io.netlibs.asterisk.ari.client.frame.MediaUri;
import io.netlibs.asterisk.ari.client.frame.PlayInterruptedException;

class StasisChannel implements LogicalChannel {

  private final ChannelContext channel;

  StasisChannel(final ChannelContext channel) {
    this.channel = Objects.requireNonNull(channel);
  }

  @Override
  public void play(final Iterator<MediaUri> media) throws PlayInterruptedException {
    throw new IllegalArgumentException();
  }

  @Override
  public <T> T read(final DtmfCollector<T> collector) throws InterruptedException {
    // TODO Auto-generated method stub
    return null;
  }

}
