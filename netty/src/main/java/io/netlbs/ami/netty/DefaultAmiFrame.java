package io.netlbs.ami.netty;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import java.util.function.BiConsumer;

import io.netlbs.ami.AmiFrame;
import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.DefaultHeadersImpl;
import io.netty.util.AsciiString;

public final class DefaultAmiFrame extends DefaultHeaders<CharSequence, CharSequence, DefaultHeadersImpl<CharSequence, CharSequence>> implements AmiFrame {

  // validates event header names.
  private static final NameValidator<CharSequence> nameValidator = (name) -> checkNotNull(name, "name");

  private DefaultAmiFrame() {
    super(AsciiString.CASE_INSENSITIVE_HASHER, CharSequenceValueConverter.INSTANCE, nameValidator);
  }

  public static DefaultAmiFrame newFrame() {
    return new DefaultAmiFrame();
  }

  @Override
  public void forEach(BiConsumer<CharSequence, CharSequence> action) {
    super.forEach(e -> action.accept(e.getKey(), e.getValue()));
  }

  @Override
  public String getOrDefault(String headerName, String defaultValue) {
    CharSequence res = super.get(headerName, defaultValue);
    if (res == null)
      return null;
    return res.toString();
  }


}
