package io.netlbs.ami.netty;

import static java.nio.charset.StandardCharsets.US_ASCII;

import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;

import io.netlbs.ami.AmiVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * "standard" AMI protocol framing version.
 */

public class AmiFrameDecoder extends SimpleChannelInboundHandler<ByteBuf> {

  private MapSplitter splitter = Splitter.on("\r\n").withKeyValueSeparator(Splitter.onPattern("[\\t ]*:[\t ]*").limit(2).trimResults());

  public AmiFrameDecoder(AmiVersion version) {
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    DefaultAmiFrame frame = DefaultAmiFrame.newFrame();
    splitter.split(msg.toString(US_ASCII)).forEach(frame::add);
    ctx.fireChannelRead(frame);
  }

}
