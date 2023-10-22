package io.netlibs.asterisk.ari.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.netlibs.asterisk.ari.stasis.DtmfDigit;

class DtmfDigitTest {

  @ParameterizedTest
  @EnumSource(DtmfDigit.class)
  void validateDigit(final DtmfDigit digit) {
    assertThat(digit.toString()).hasSize(1).isEqualTo(Character.toString(digit.character()));
    assertThat(DtmfDigit.of(digit.toString().charAt(0))).isEqualTo(digit);
  }

}
