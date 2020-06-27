package io.netlibs.ami.client;

import java.nio.charset.StandardCharsets;

import io.netlibs.ami.api.AmiChannelStatusMessage;
import io.netlibs.ami.api.AmiMessage;
import io.netlibs.ami.api.AmiVersion;
import io.netlibs.ami.netty.AmiFrameCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class AmiClientHandler extends ChannelInitializer<Channel> {

  private static final ByteBuf PROTOCOL_PREFIX_BUF =
    Unpooled.wrappedBuffer(
      "Asterisk Call Manager/6.".getBytes(StandardCharsets.US_ASCII)).asReadOnly();

  private static final ByteBuf FRAME_SEP = Unpooled.wrappedBuffer("\r\n\r\n".getBytes()).asReadOnly();

  private final BehaviorProcessor<AmiMessage> incoming;

  private SslContext sslctx;

  public AmiClientHandler(BehaviorProcessor<AmiMessage> incoming, SslContext sslctx) {
    this.incoming = incoming;
    this.sslctx = sslctx;
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {

    ChannelPipeline p = ch.pipeline();

    p.addLast("ssl", sslctx.newHandler(ch.alloc()));

    // decode based on line initially, as we want to read the first one to adapt to the correct
    // protocol before sending a login frame.
    p.addLast("frameDecoder", new LineBasedFrameDecoder(65535, true, true));

    // protocol selector.
    p.addLast(new LoggingHandler(getClass(), LogLevel.TRACE));

    // protocol selector.
    p.addLast(new AmiProtocolSelector());

    // protocol selector.
    p.addLast(new LoggingHandler(getClass(), LogLevel.DEBUG));

  }

  /**
   * receives a single (trimmed) line which is the initial banner. we set a timeout incase it is not
   * received.
   */

  private class AmiProtocolSelector extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

      if (ByteBufUtil.equals(msg, msg.readerIndex(), PROTOCOL_PREFIX_BUF, 0, PROTOCOL_PREFIX_BUF.readableBytes())) {

        ChannelPipeline p = ctx.pipeline();

        // replace the frameDecoder to split on 2xCRLF instead of a single newline for the banner
        p.replace("frameDecoder", "frameDecoder", new DelimiterBasedFrameDecoder(Short.MAX_VALUE, true, true, FRAME_SEP));

        // add a codec handler.
        p.replace(this, "codec", new AmiFrameCodec(AmiVersion.ASTERISK_6));

        // notify.
        incoming.onNext(AmiChannelStatusMessage.connected(AmiVersion.ASTERISK_6));

        p.addLast("dispatcher", new MessageDispatcher(incoming));

        ctx.fireChannelRegistered();

      }
      else {

        throw new IllegalArgumentException(String.format("unexpected protocol frame: '%s'", msg));

      }

    }

  }

}
