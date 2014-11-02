public class Casts_true_assert {

  public static void main(String[] args) {
    checkFloatCast();
    checkCastWithOverflow();
  }

  private static void checkFloatCast() {
    int n = (int) 1.00001;

    assert n == 1; //always true
  }

  private static void checkCastWithOverflow() {
    byte n = (byte) 257; // n2 = 1

    assert n == 1; // always true
  }
}
