package io.netlibs.asterisk.ari.stasis;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netlibs.asterisk.ari.client.frame.DtmfCollector;

/**
 * Dispatches the DTMF event to listeners, potentially causing some actions to happen.
 *
 * Only a single listener may be active at any point in time. An attempt to listen for DTMF events when there is already a listener will
 * throw an {@link IllegalStateException}.
 *
 */

final class DtmfBuffer {

  private static final Logger LOG = LoggerFactory.getLogger(DtmfBuffer.class);

  // protects the buffer itself, and listener.
  private final Lock lock = new ReentrantLock();

  // set when we add something to the buffer.
  private final Condition hasItem = this.lock.newCondition();

  // buffer which holds all the enqueued (un-processed) DTMF events.
  private final StringBuilder buffer = new StringBuilder();

  // true when this buffer is closed and can no longer be used.
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * start collecting DTMF events, blocking until any are available or the buffer is closed.
   *
   * @return The value returned by the accumulator.
   *
   * @throws InterruptedException
   *           If interrupted before the collector completed.
   *
   * @throws StasisClosedException
   *           The statis application/channel is closed before the buffer completed.
   */

  public <R> R listen(final DtmfCollector<R> collector) throws InterruptedException, StasisClosedException {

    if (this.closed.get()) {
      throw new StasisClosedException();
    }

    int lastLength = 0;

    while (!this.closed.get()) {

      LOG.debug("Waiting for DTMF event");

      final String read = this.readBuffer(lastLength);

      final Optional<R> result = collector.accumulate(read);

      if (result.isPresent()) {
        return result.get();
      }

      lastLength = read.length();

    }

    throw new StasisClosedException();

  }

  /**
   * Waits until the buffer has changed length, or interrupted.
   *
   * @param length
   *          the current length of the buffer.
   * @return A snapshot of the buffer after it has changed size.
   * @throws StasisClosedException
   *           The channel was closed.
   */

  private String readBuffer(final int length) throws InterruptedException, StasisClosedException {
    this.lock.lock();
    try {
      if (this.closed.get()) {
        throw new StasisClosedException();
      }
      while (this.buffer.length() == length) {
        this.hasItem.await();
        if (this.closed.get()) {
          throw new StasisClosedException();
        }
      }
      return this.buffer.toString();
    }
    finally {
      this.lock.unlock();
    }
  }

  /**
   * Close the buffer, indicating that there will be no more events possible. Any listeners will return from their listen.
   *
   * Calling multiple times will have no effect.
   *
   */

  public void close() {
    this.lock.lock();
    try {

      //
      if (this.closed.get()) {
        // already closed. do nothing.
        return;
      }

      LOG.info("closing DtmfBuffer");
      this.closed.set(true);

      // wake up the listener
      this.hasItem.signal();

    }
    finally {
      this.lock.unlock();
    }
  }

  /**
   * called by the network event receiver when there is a DTMF to be pushed into the buffer.
   *
   * this triggers notification of a waiting (virtual) thread, which then may act on the buffer content.
   *
   */

  public void add(final DtmfEvent dtmfEvent) {
    this.lock.lock();
    try {
      Preconditions.checkState(!this.closed.get());
      this.buffer.append(dtmfEvent.digit().character());
      this.hasItem.signal();
    }
    finally {
      this.lock.unlock();
    }
  }

}
