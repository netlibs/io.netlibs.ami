package io.netlibs.asterisk.ari.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PJSIPTest {

  @Test
  void test() {

    assertThat(PJSIP.of("hello")).hasToString("PJSIP/hello");
    assertThat(PJSIP.of("hello", "sip:test@domain.com")).hasToString("PJSIP/hello/sip:test@domain.com");

  }

}
