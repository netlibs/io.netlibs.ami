package io.netlibs.ami.netty;

import java.nio.charset.StandardCharsets;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import io.netlibs.ami.api.AmiVersion;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * "standard" AMI protocol framing version.
 */

public class AmiFrameDecoder extends SimpleChannelInboundHandler<ByteBuf> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmiFrameDecoder.class);
  private Splitter splitter = Splitter.on("\r\n");

  public AmiFrameDecoder(AmiVersion version) {
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

    // first, split on newlines.

    DefaultAmiFrame frame = DefaultAmiFrame.newFrame();

    splitter.split(msg.toString(StandardCharsets.UTF_8))
      .forEach(line -> {

        if (Strings.isNullOrEmpty(line)) {
          return;
        }

        int idx = line.indexOf(':');

        if (idx == -1) {
          log.error("frame line was invalid: [{}]", line);
          return;
        }

        String name = line.substring(idx).trim();
        String value = line.substring(idx + 1).trim();

        if (frame.contains(name)) {
          log.warn("duplicate key '{}', values: {}, adding '{}'", name, frame.getAll(name), value);
        }

        frame.add(name, value);

      });

    ctx.fireChannelRead(frame);

  }

}
