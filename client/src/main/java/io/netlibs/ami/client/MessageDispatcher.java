package io.netlibs.ami.client;

import io.netlibs.ami.api.AmiFrame;
import io.netlibs.ami.api.AmiMessage;
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
