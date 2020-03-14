package io.netlibs.ami.client;

import io.netlbs.ami.AmiFrame;
import io.netlbs.ami.AmiMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.reactivex.rxjava3.processors.FlowableProcessor;

public class MessageDispatcher extends SimpleChannelInboundHandler<AmiFrame> {

  private final FlowableProcessor<AmiMessage> target;

  public MessageDispatcher(FlowableProcessor<AmiMessage> target) {
    this.target = target;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, AmiFrame msg) throws Exception {
    this.target.onNext(msg);
  }

}
