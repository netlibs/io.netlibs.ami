package io.netlibs.asterisk.ari.stasis;

public enum DtmfDigit {

  ZERO('0'),
  ONE('1'),
  TWO('2'),
  THREE('3'),
  FOUR('4'),
  FIVE('5'),
  SIX('6'),
  SEVEN('7'),
  EIGHT('8'),
  NINE('9'),

  POUND('#'),
  STAR('*'),

  A('A'),
  B('B'),
  C('C'),
  D('D');

  private final char ch;

  DtmfDigit(final char ch) {
    this.ch = ch;
  }

  @Override
  public String toString() {
    return Character.toString(this.ch);
  }

  public char character() {
    return this.ch;
  }

  public static DtmfDigit of(final char digit) {
    return switch (digit) {
      case '0' -> ZERO;
      case '1' -> ONE;
      case '2' -> TWO;
      case '3' -> THREE;
      case '4' -> FOUR;
      case '5' -> FIVE;
      case '6' -> SIX;
      case '7' -> SEVEN;
      case '8' -> EIGHT;
      case '9' -> NINE;
      case '*' -> STAR;
      case '#' -> POUND;
      case 'A' -> A;
      case 'B' -> B;
      case 'C' -> C;
      case 'D' -> D;
      default -> throw new IllegalArgumentException("Invalid DTMF digit: " + Character.toString(digit));
    };
  }

}
