package io.netlibs.ami.api;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

public interface AmiFrame extends AmiMessage {

  void forEach(BiConsumer<CharSequence, CharSequence> action);

  String getOrDefault(String headerName, String defaultValue);

  Iterator<Entry<CharSequence, CharSequence>> iterator();

  boolean contains(CharSequence string);

  CharSequence get(CharSequence string);

  /**
   * checks if a single head exists with the specified name and case sensitive value.
   * 
   * @param haystackName
   * @param needle
   * @return
   */

  default boolean scalarContains(CharSequence haystackName, CharSequence needle) {
    CharSequence haystackValue = get(haystackName);
    return (haystackValue != null) && (CharSequence.compare(haystackValue, needle) == 0);
  }

  boolean remove(CharSequence headerName);

}
