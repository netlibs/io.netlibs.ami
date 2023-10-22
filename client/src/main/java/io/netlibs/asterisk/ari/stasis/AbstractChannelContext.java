package io.netlibs.asterisk.ari.stasis;

import java.time.Duration;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.client.frame.DtmfCollector;
import io.netlibs.asterisk.ari.commands.PlayParams;
import io.netlibs.asterisk.ari.commands.RecordParams;
import io.netlibs.asterisk.ari.events.Channel;

class AbstractChannelContext implements ChannelContext {

  protected final AriClient ari;
  private final Channel channel;
  protected final DtmfBuffer dtmfBuffer = new DtmfBuffer();

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
    this.ari.dial(this.channel.id());
  }

  @Override
  public void recordFile(final RecordParams params) throws InterruptedException {
    throw new UnsupportedOperationException("Unimplemented Method: ChannelContext.record invoked.");
  }

  @Override
  public void play(final PlayParams params) throws InterruptedException {
    throw new UnsupportedOperationException("Unimplemented Method: ChannelContext.play invoked.");
  }

  @Override
  public void hangup() throws InterruptedException {
    this.ari.hangup(this.channel.id());
  }

  @Override
  public <R> R read(final DtmfCollector<R> collector) throws InterruptedException, StasisClosedException {
    return this.dtmfBuffer.listen(collector);
  }

}
