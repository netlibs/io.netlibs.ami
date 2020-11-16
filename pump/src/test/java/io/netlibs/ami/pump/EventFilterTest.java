package io.netlibs.ami.pump;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class EventFilterTest {

  @Test
  void test() {
    EventFilter f = new EventFilter(Arrays.asList("Hel*", "-Hello"));
    assertFalse(f.test("Hello"));
    assertTrue(f.test("Helloss"));
    System.err.println(f);
  }

  @Test
  void test1() {
    EventFilter f = new EventFilter(Arrays.asList("RTCP*"));
    assertFalse(f.test(""));
    assertFalse(f.test("newchannel"));
    assertTrue(f.test("RTCPEvent"));
    System.err.println(f);
  }

}
