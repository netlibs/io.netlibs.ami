package io.netlibs.ami.netty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    String body = msg.toString(StandardCharsets.UTF_8);

    ArrayList<String> errors = null;

    for (String line : splitter.split(body)) {

      if (Strings.isNullOrEmpty(line)) {
        continue;
      }

      int idx = line.indexOf(':');

      if (idx == -1) {
        if (errors == null)
          errors = new ArrayList<>();
        errors.add(line);
        continue;
      }

      String name = line.substring(idx).trim();
      String value = line.substring(idx + 1).trim();

      if (frame.contains(name)) {
        List<CharSequence> existing = frame.getAll(name);
        if (existing.contains(value)) {
          log.debug("duplicate value for header '{}': '{}'", name, value);
          // just duplicate wiht same value, ignore.
          return;
        }
        log.warn("duplicate key '{}', values: {}, adding '{}'", name, frame.getAll(name), value);
      }

      frame.add(name, value);

    }

    if (errors != null) {
      log.warn("found {} invalid lines processing message:\n{}\n", errors.size(), body);
      errors.forEach(line -> log.warn("offending line: [{}]", line));
    }

    ctx.fireChannelRead(frame);

  }

}
