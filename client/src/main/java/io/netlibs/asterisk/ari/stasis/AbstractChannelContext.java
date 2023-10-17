package io.netlibs.asterisk.ari.stasis;

import java.time.Duration;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.commands.PlayParams;
import io.netlibs.asterisk.ari.commands.RecordParams;
import io.netlibs.asterisk.ari.events.Channel;

class AbstractChannelContext implements ChannelContext {

  protected final AriClient ari;
  private final Channel channel;

  public AbstractChannelContext(final AriClient ari, final Channel channel) {
    this.ari = ari;
    this.channel = channel;
  }

  @Override
  public void ring() throws InterruptedException {
    this.ari.ring(this.channel.id());
  }

  @Override
  public void answer() throws InterruptedException {
    this.ari.answer(this.channel.id());
  }

  @Override
  public void dial(final Duration timeout) throws InterruptedException {
    this.ari.dial(this.channel.id(), null, timeout);
  }

  @Override
  public void record(final RecordParams record) throws InterruptedException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented Method: ChannelContext.record invoked.");
  }

  @Override
  public void play(final PlayParams params) throws InterruptedException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented Method: ChannelContext.play invoked.");
  }

  @Override
  public void hangup() throws InterruptedException {
    this.ari.hangup(this.channel.id());
  }

}
