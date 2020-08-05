package io.netlibs.ami.netty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
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
  private static final int HNAME_MAX_LEN = 32;
  private Splitter splitter = Splitter.on("\r\n");
  private CharMatcher validHeaderName = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('0', '9')).or(CharMatcher.anyOf("-_."));

  public AmiFrameDecoder(AmiVersion version) {
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

    // first, split on newlines.

    DefaultAmiFrame frame = DefaultAmiFrame.newFrame();

    String body = msg.toString(StandardCharsets.UTF_8);

    String lastHeaderName = null;

    ArrayList<String> errors = null;

    for (String line : splitter.split(body)) {

      if (Strings.isNullOrEmpty(line)) {
        // shouldn't happen...
        continue;
      }

      int idx = line.indexOf(':');

      if ((idx == -1) || !isValidHeaderName(line.substring(0, idx).trim())) {

        if (lastHeaderName.equalsIgnoreCase("AppData")) {
          // horrible hack to work around asterisk multi-line app data.
          List<CharSequence> value = frame.getAllAndRemove(lastHeaderName);
          value.add(line);
          frame.set(lastHeaderName, Joiner.on("\r\n").join(value));
          continue;
        }

        if (errors == null) {
          errors = new ArrayList<>();
        }

        errors.add(line);
        continue;

      }

      String name = line.substring(0, idx).trim();
      String value = line.substring(idx + 1).trim();

      lastHeaderName = name;

      if (frame.contains(name)) {
        List<CharSequence> existing = frame.getAll(name);
        if (existing.contains(value)) {
          // just duplicate with same value, ignore.
          log.debug("duplicate value for header '{}': '{}'", name, value);
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

  private boolean isValidHeaderName(String value) {
    return ((value.length() > 0) && (value.length() < HNAME_MAX_LEN)) && validHeaderName.matchesAllOf(value.toLowerCase());
  }

}
