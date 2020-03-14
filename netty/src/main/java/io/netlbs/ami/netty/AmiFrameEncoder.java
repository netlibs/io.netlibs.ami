package io.netlbs.ami.netty;

import static io.netty.handler.codec.http.HttpConstants.COLON;
import static io.netty.handler.codec.http.HttpConstants.CR;
import static io.netty.handler.codec.http.HttpConstants.LF;
import static io.netty.handler.codec.http.HttpConstants.SP;

import java.util.Iterator;
import java.util.Map.Entry;

import io.netlbs.ami.AmiFrame;
import io.netlbs.ami.AmiVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

/**
 * "standard" AMI protocol framing encoder.
 */

public class AmiFrameEncoder extends MessageToByteEncoder<AmiFrame> {

  private static final int CRLF_SHORT = (CR << 8) | LF;
  private static final int COLON_AND_SPACE_SHORT = (COLON << 8) | SP;

  public AmiFrameEncoder(AmiVersion version) {
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, AmiFrame frame, ByteBuf out) throws Exception {

    Iterator<Entry<CharSequence, CharSequence>> iter = frame.iterator();

    while (iter.hasNext()) {
      Entry<CharSequence, CharSequence> header = iter.next();
      encoderHeader(header.getKey(), header.getValue(), out);
    }

    ByteBufUtil.writeShortBE(out, CRLF_SHORT);

  }

  static void encoderHeader(CharSequence name, CharSequence value, ByteBuf buf) {
    final int nameLen = name.length();
    final int valueLen = value.length();
    final int entryLen = nameLen + valueLen + 4;
    buf.ensureWritable(entryLen);
    int offset = buf.writerIndex();
    writeAscii(buf, offset, name);
    offset += nameLen;
    ByteBufUtil.setShortBE(buf, offset, COLON_AND_SPACE_SHORT);
    offset += 2;
    writeAscii(buf, offset, value);
    offset += valueLen;
    ByteBufUtil.setShortBE(buf, offset, CRLF_SHORT);
    offset += 2;
    buf.writerIndex(offset);
  }

  private static void writeAscii(ByteBuf buf, int offset, CharSequence value) {
    if (value instanceof AsciiString) {
      ByteBufUtil.copy((AsciiString) value, 0, buf, offset, value.length());
    }
    else {
      buf.setCharSequence(offset, value, CharsetUtil.US_ASCII);
    }
  }

}
