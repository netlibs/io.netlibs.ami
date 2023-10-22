package io.netlibs.asterisk.ari.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netlibs.asterisk.ari.stasis.DtmfDigit;

class DtmfEventTest {

  @Test
  void test() {
    assertEquals("0", DtmfDigit.ZERO.toString());
  }

}
