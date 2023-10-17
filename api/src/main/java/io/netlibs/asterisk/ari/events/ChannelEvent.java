package io.netlibs.asterisk.ari.events;

public sealed interface ChannelEvent permits
    StasisStart, StasisEnd, ChannelStateChange, ChannelHangupRequest,
    ChannelCallerId, ChannelConnectedLine, ChannelDestroyed, ChannelDialplan,
    ChannelDtmfReceived, ChannelEnteredBridge, ChannelLeftBridge, ChannelTalkingStarted, ChannelTalkingFinished,
    ChannelHold, ChannelUnhold {

  Channel channel();

}
