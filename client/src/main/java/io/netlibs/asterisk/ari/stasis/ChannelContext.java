package io.netlibs.asterisk.ari.stasis;

import java.time.Duration;

import io.netlibs.asterisk.ari.commands.PlayParams;
import io.netlibs.asterisk.ari.commands.RecordParams;

public interface ChannelContext {

  /**
   * indicate ringing on this channel (which must be an incoming one).
   */

  void ring() throws InterruptedException;

  /**
   * answer the call.
   */

  void answer() throws InterruptedException;

  /**
   * calls the dial command on the channel (which must be an outgoing one). will complete once the
   * channel is connected, or fails.
   */

  void dial(Duration timeout) throws InterruptedException;

  /**
   * perform recording on this channel. this is not the same as channel-wide recording which should
   * be done with snoop, and it asynchronous.
   */

  void record(RecordParams record) throws InterruptedException;

  /**
   * play media, waiting until it completes.
   *
   * if you wish to receive DTMF tones or other events, start another task and interrupt this one,
   * followed by sotpping playback (if required).
   *
   */

  void play(PlayParams params) throws InterruptedException;

  /**
   *
   */

  // answer
  // hold/unhold
  // moh/ start/stop - optional mohClass
  // silence
  // send dtmf
  // mute/unmute
  // snoop

  // continue in dialplan
  // move to another stasis app
  // redirect channel

  // variable
  // snoop

  /**
   * terminate this call.
   */

  void hangup() throws InterruptedException;

}
