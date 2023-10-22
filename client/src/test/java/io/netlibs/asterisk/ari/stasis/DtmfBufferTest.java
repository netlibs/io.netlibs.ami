package io.netlibs.asterisk.ari.stasis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.netlibs.asterisk.ari.client.frame.DtmfCollectors;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class DtmfBufferTest {

  @Test
  void test() throws InterruptedException, ExecutionException, TimeoutException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      final DtmfBuffer buffer = new DtmfBuffer();
      try {
        final Subtask<String> res = scope.fork(() -> buffer.listen(DtmfCollectors.fixedLength(1)));
        buffer.add(new DtmfEvent(DtmfDigit.A, Duration.ofSeconds(1)));
        scope.joinUntil(Instant.now().plusSeconds(1));
        scope.throwIfFailed();
        assertThat(res.get()).isEqualTo("A");
        buffer.close();
      }
      finally {
        scope.shutdown();
        scope.join();
      }
    }
  }

  @Test
  void testClose() throws InterruptedException, ExecutionException, TimeoutException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      final DtmfBuffer buffer = new DtmfBuffer();
      try {
        final Subtask<String> res = scope.fork(() -> buffer.listen(DtmfCollectors.fixedLength(2)));
        buffer.add(new DtmfEvent(DtmfDigit.A, Duration.ofSeconds(1)));
        // wait until we get signal indicating
        buffer.close();
        scope.joinUntil(Instant.now().plusSeconds(1));
        assertThatThrownBy(() -> scope.throwIfFailed())
          .isInstanceOf(ExecutionException.class)
          .extracting(Throwable::getCause)
          .isInstanceOf(StasisClosedException.class);
      }
      finally {
        scope.shutdown();
        scope.join();
      }
    }
  }

}
