package io.netlibs.asterisk.ari.client;

import java.net.http.WebSocket;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netlibs.asterisk.ari.commands.ChannelEndpoint;
import io.netlibs.asterisk.ari.commands.DialParams;
import io.netlibs.asterisk.ari.commands.ExternalMediaParams;
import io.netlibs.asterisk.ari.commands.OriginateParams;
import io.netlibs.asterisk.ari.commands.RecordParams;
import io.netlibs.asterisk.ari.commands.SnoopChannelParams;
import io.netlibs.asterisk.ari.events.Bridge;
import io.netlibs.asterisk.ari.events.Channel;
import io.netlibs.asterisk.ari.events.Event;

public interface AriClient {

  /**
   * Register and start receiving events.
   */

  CompletableFuture<WebSocket> events(String appId, Consumer<Event> handler);

  /**
   * create a new outgoing channel.
   */

  Channel createChannel(OriginateParams params) throws InterruptedException;

  void answer(String channelId) throws InterruptedException;

  void ring(String channelId) throws InterruptedException;

  void ringStop(String channelId) throws InterruptedException;

  void hold(String channelId) throws InterruptedException;

  void unhold(String channelId) throws InterruptedException;

  void startMoh(String channelId) throws InterruptedException;

  void stopMoh(String channelId) throws InterruptedException;

  void startSilence(String channelId) throws InterruptedException;

  void stopSilence(String channelId) throws InterruptedException;

  void sendDTMF(String channelId, String dtmf) throws InterruptedException;

  String getChannelVar(String channelId, String variable) throws InterruptedException;

  void setChannelVar(String channelId, String variable, String value) throws InterruptedException;

  Channel snoopChannelWithId(String channelId, SnoopChannelParams params) throws InterruptedException;

  Channel externalMedia(String channelId, ExternalMediaParams params) throws InterruptedException;

  /**
   * dial a channel.
   */

  void dial(DialParams params) throws InterruptedException;

  /**
   * call dial on a channel.
   */

  default void dial(final String channelId) throws InterruptedException {
    this.dial(
      DialParams.builder()
        .channelId(channelId)
        .build());
  }

  /**
   * disconnect this call.
   */

  void hangup(String channelId, String reason) throws InterruptedException;

  default void hangup(final String channelId) throws InterruptedException {
    this.hangup(channelId, null);
  }

  void redirect(String channelId, ChannelEndpoint endpoint) throws InterruptedException;

  //

  void startRecording(String channelId, RecordParams params) throws InterruptedException;

  enum RecordingOperation {
    PAUSE,
    UNPAUSE,
    MUTE,
    UNMUTE,
  }

  void controlRecording(String recordingName, RecordingOperation operation) throws InterruptedException;

  void stopRecording(String recordingName) throws InterruptedException;

  void cancelRecording(String recordingName) throws InterruptedException;

  /**
   * send a play, which returns a Play ID that can be used to monitor and pause/stop it.
   */

  void playWithId(String channelId, String playbackId, String media) throws InterruptedException;

  enum PlaybackOperation {
    RESTART,
    PAUSE,
    UNPAUSE,
    REVERSE,
    FORWARD,
  }

  void controlPlayback(String playbackId, PlaybackOperation op) throws InterruptedException;

  void stopPlayback(String playbackId) throws InterruptedException;

  //

  //

  Bridge createBridge() throws InterruptedException;

  void addChannels(String bridgeId, Set<String> channelIds) throws InterruptedException;

  default void addChannels(final String bridgeId, final String... ids) throws InterruptedException {
    this.addChannels(bridgeId, Set.of(ids));
  }

}
