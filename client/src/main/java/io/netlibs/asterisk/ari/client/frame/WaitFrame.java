package io.netlibs.asterisk.ari.client.frame;

/**
 * service this channel with a sub-frame, and wait for a signal with a bridge.
 *
 * the servicing frame provider will be destroyed once there is a signal of a bridge.
 *
 */

public record WaitFrame() implements ChannelFrame {

}
