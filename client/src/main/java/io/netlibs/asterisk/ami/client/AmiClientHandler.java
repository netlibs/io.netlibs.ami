package io.netlibs.asterisk.ami.client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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

public class AmiClientHandler extends ChannelInitializer<Channel> {

  private static final ByteBuf PROTOCOL_PREFIX_BUF =
    Unpooled.wrappedBuffer(
      "Asterisk Call Manager/".getBytes(StandardCharsets.US_ASCII)).asReadOnly();

  private static final ByteBuf FRAME_SEP = Unpooled.wrappedBuffer("\r\n\r\n".getBytes()).asReadOnly();

  private SslContext sslctx;

  private CompletableFuture<Channel> handler;

  public AmiClientHandler(CompletableFuture<Channel> chf, SslContext sslctx) {
    this.handler = chf;
    this.sslctx = sslctx;
  }

  public AmiClientHandler(CompletableFuture<Channel> chf) {
    this(chf, null);
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {

    ChannelPipeline p = ch.pipeline();

    if (sslctx != null) {
      p.addLast("ssl", sslctx.newHandler(ch.alloc()));
    }

    // decode based on line initially, as we want to read the first one to adapt to the correct
    // protocol before sending a login frame.
    p.addLast("frameDecoder", new LineBasedFrameDecoder(65535, true, true));

    // protocol selector.
    p.addLast(new LoggingHandler(getClass(), LogLevel.TRACE));

    // protocol selector.
    p.addLast(new AmiProtocolSelector());

  }

  /**
   * receives a single (trimmed) line which is the initial banner. we set a timeout incase it is not
   * received.
   */

  private class AmiProtocolSelector extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      if (ctx.channel().isActive() && !ctx.channel().config().isAutoRead()) {
        ctx.read();
      }
      super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (!ctx.channel().config().isAutoRead()) {
        ctx.read();
      }
      super.channelRegistered(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

      if (msg.readableBytes() > PROTOCOL_PREFIX_BUF.readableBytes() && ByteBufUtil.equals(msg.slice(msg.readerIndex(), PROTOCOL_PREFIX_BUF.readableBytes()), msg.readerIndex(), PROTOCOL_PREFIX_BUF, 0, PROTOCOL_PREFIX_BUF.readableBytes())) {

        ChannelPipeline p = ctx.pipeline();

        // replace the frameDecoder to split on 2xCRLF instead of a single newline for the banner
        p.replace("frameDecoder", "frameDecoder", new DelimiterBasedFrameDecoder(Short.MAX_VALUE, true, true, FRAME_SEP));

        // add a codec handler.
        p.replace(this, "codec", new AmiFrameCodec(AmiVersion.ASTERISK_6));

        handler.complete(ctx.channel());

      }
      else {
        handler.completeExceptionally(new IllegalArgumentException(String.format("unexpected protocol frame: '%s'", msg)));
      }

    }

  }

}
