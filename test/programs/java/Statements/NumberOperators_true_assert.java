public class NumberOperators_true_assert {

  public static void main(String[] args) {
    int a = 1;

    a *= 1;
    a += 1;
    a -= 1;
    a /= 1;

    assert a == 1;
    assert a > 0;
    assert a < 2;
    assert a != 2;

    a = a * 1;
    a = a + 1;
    a = a - 1;
    a = a / 1;

    assert a == 1;
    assert a > 0;
    assert a < 2;
    assert a != 2;

    a = a << 1;

    assert a == 2;

    a = -1;
    a = a >> 1;
    assert a == -1;

    a = -1;
    a = a >>> 1;
    assert a == 2147483647;
    
    a = 1;
    a &= 3;
    assert a == 1;

    a = 1;
    a |= 2;
    assert a == 3;

    a ^= 1;
    assert a == 2;
  }

}
