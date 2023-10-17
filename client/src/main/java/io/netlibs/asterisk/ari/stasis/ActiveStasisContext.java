package io.netlibs.asterisk.ari.stasis;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicReference;

import io.netlibs.asterisk.ari.client.AriClient;
import io.netlibs.asterisk.ari.events.Channel;
import io.netlibs.asterisk.ari.events.ChannelEvent;
import io.netlibs.asterisk.ari.events.Event;
import io.netlibs.asterisk.ari.events.StasisEnd;
import io.netlibs.asterisk.ari.events.StasisStart;

class ActiveStasisContext extends AbstractChannelContext implements StasisContext {

  final LinkedTransferQueue<Event> queue = new LinkedTransferQueue<>();

  private final AriClient ari;
  private final Thread thread;

  private final StasisStart start;

  // set in the receiver thread as soon as we receive StasisEnd. it's also added into the queue.
  // having here allows us to do lookahead easily to see if the channel has already been ended for
  // us.
  private final AtomicReference<StasisEnd> end = new AtomicReference<>();

  // some reasonable definition of the most recent state of the channel, based on incoming events.
  private final AtomicReference<Channel> channel = new AtomicReference<>();

  private final String channelId;

  private final StartParams startParams;

  ActiveStasisContext(final Thread thread, final AriClient ari, final StasisStart startEvent) {

    super(ari, startEvent.channel());

    this.start = startEvent;
    this.thread = thread;
    this.ari = ari;

    this.channel.set(startEvent.channel());

    this.startParams =
      new StartParams(
        startEvent.application(),
        startEvent.args()
          .stream()
          .map(String::trim)
          .toList());

    this.channelId = startEvent.channel().id();

  }

  /**
   * true if we have received a StasisEnd. this is false if we end the call but have not yet
   * received the StasisEnd.
   */

  public boolean hasEnded() {
    return this.end.get() != null;
  }

  /*
   * internally enqueue an event, called on the receiver side of the thread not the processor.
   */

  void enqueue(final Event e) {
    switch (e) {
      case final StasisEnd f:
        this.end.set(f);
        this.queue.add(e);
        break;
      case final ChannelEvent f:
        this.channel.set(f.channel());
        this.queue.add(e);
        break;
      default:
        this.queue.add(e);
        break;
    }
  }

  @Override
  public StartParams startParams() {
    return this.startParams;
  }

  @Override
  public String channelId() {
    return this.channelId;
  }

  @Override
  public Channel snapshot() {
    return this.channel.get();
  }

  @Override
  public Event read() throws InterruptedException {
    return this.queue.take();
  }

}
