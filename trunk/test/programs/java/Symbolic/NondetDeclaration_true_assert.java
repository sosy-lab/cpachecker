import java.lang.Math;

public class NondetDeclaration_true_assert {

  public static void main(String[] args) {
    int a = (int) (Math.random() * 10);
    int b = a;

    assert a == b;
  }
}
