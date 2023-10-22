package io.netlibs.asterisk.ari.client.frame;

public sealed interface ChannelFrame permits WaitFrame, CollectFrame, RecordFrame, PlayFrame, RingingFrame {

}
