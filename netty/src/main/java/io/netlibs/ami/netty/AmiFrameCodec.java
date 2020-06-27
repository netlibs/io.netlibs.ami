package io.netlibs.ami.netty;

import io.netlibs.ami.api.AmiVersion;
import io.netty.channel.CombinedChannelDuplexHandler;

public class AmiFrameCodec extends CombinedChannelDuplexHandler<AmiFrameDecoder, AmiFrameEncoder> {

  public AmiFrameCodec(AmiVersion version) {
    super(new AmiFrameDecoder(version), new AmiFrameEncoder(version));
  }

}
