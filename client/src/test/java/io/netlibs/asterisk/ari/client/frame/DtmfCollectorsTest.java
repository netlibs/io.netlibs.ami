package io.netlibs.asterisk.ari.client.frame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.netlibs.asterisk.ari.stasis.DtmfDigit;

class DtmfCollectorsTest {

  @Test
  void shouldAccumulateAndReturnStringWhenLengthIsReachedWithoutStop() {
    final DtmfCollector<String> fixedLength = DtmfCollectors.fixedLength(3, Set.of());
    assertThat(fixedLength.accumulate("A")).isEmpty();
    assertThat(fixedLength.accumulate("AB")).isEmpty();
    assertThat(fixedLength.accumulate("ABC")).hasValue("ABC");
  }

  @Test
  void shouldAccumulateAndReturnStringWhenLengthIsReachedWithStop() {
    final DtmfCollector<String> fixedLength = DtmfCollectors.fixedLength(3, Set.of(DtmfDigit.POUND));
    assertThat(fixedLength.accumulate("A")).isEmpty();
    assertThat(fixedLength.accumulate("AB")).isEmpty();
    assertThat(fixedLength.accumulate("ABC")).hasValue("ABC");
  }

  @Test
  void shouldNotReturnStringWhenLengthIsNotReached() {
    final DtmfCollector<String> fixedLength = DtmfCollectors.fixedLength(4, Set.of(DtmfDigit.POUND));
    assertThat(fixedLength.accumulate("A")).isEmpty();
    assertThat(fixedLength.accumulate("AB")).isEmpty();
    assertThat(fixedLength.accumulate("ABC")).isEmpty();
  }

  @Test
  void shouldReturnWhenStopCharMatchedBeforeComplete() {
    final DtmfCollector<String> fixedLength = DtmfCollectors.fixedLength(4, Set.of(DtmfDigit.POUND));
    assertThat(fixedLength.accumulate("A")).isEmpty();
    assertThat(fixedLength.accumulate("AB")).isEmpty();
    assertThat(fixedLength.accumulate("AB#")).hasValue("AB#");
  }

  @Test
  void shouldReturnStopDigitStringWhenStopConditionMetAndStopDigit() {
    final DtmfCollector<String> fixedLength = DtmfCollectors.fixedLength(3, Set.of(DtmfDigit.C));
    assertThat(fixedLength.accumulate("A")).isEmpty();
    assertThat(fixedLength.accumulate("AB")).isEmpty();
    assertThat(fixedLength.accumulate("ABC")).hasValue("ABC");
  }

  @Test
  void shouldReturnEmptyOptionalWhenLengthIsZero() {
    assertThatThrownBy(() -> DtmfCollectors.fixedLength(0)).isInstanceOf(IllegalArgumentException.class);
  }

}
