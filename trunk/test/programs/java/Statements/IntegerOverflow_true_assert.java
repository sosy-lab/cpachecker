
public class IntegerOverflow_true_assert {

  public static void main(String[] args0) {
    int a = 2147483647;

    a = a + 1;
    assert a == -2147483648;

    a = a - 1;
    assert a == 2147483647;
  }
}
