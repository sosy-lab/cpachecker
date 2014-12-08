import java.lang.Math;

public class NondetAssignment_true_assert {

  public static void main(String[] args) {
    int a;
    int b;

    a = (int) Math.random();
    b = a;

    assert a == b;
  }
}
