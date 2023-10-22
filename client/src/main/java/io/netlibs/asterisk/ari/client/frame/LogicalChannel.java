package io.netlibs.asterisk.ari.client.frame;

import java.util.Iterator;

import com.google.common.collect.Iterators;

/**
 * A virtual channel represents a logical asterisk channel which can be replaced, for example in the case of an INVITE w/Replaces.
 *
 * As long as only this class is used to interact with the channel, frames which are currently active on the channel will be automatically
 * replaced and continue where the previous channel was.
 *
 */

public interface LogicalChannel {

  /**
   * Read a single DMTF event from this channel, removing it from the input buffer.
   */

  <T> T read(DtmfCollector<T> collector) throws InterruptedException;

  /**
   * Plays each of the media URIs on this channel, one by one. if the thread is interrupted, the play will be cancelled and an
   * PlayInterruptedException.
   *
   * If you wish to collect DTMF at the same time, fork another virtual thread that listens for them.
   *
   * @throw PlayInterruptedException The play was interrupted, custom exception contains the playback location where it was stopped.
   *
   */

  void play(Iterator<MediaUri> media) throws PlayInterruptedException;

  default void play(final MediaUri... media) throws PlayInterruptedException {
    this.play(Iterators.forArray(media));
  }

}
