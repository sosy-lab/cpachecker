import java.lang.Math;

public class ContradictiveIf_true_assert {

  int c;

  public static void main(String[] args) {
    int a = (int) (Math.random() * 1000);
    int b = (int) (Math.random() * 1000);

    if (a > 500) {
      if (a <= 500) {
        assert false;
      }

      a = b - 200;

      assert b == a + 200;
    }

    ContradictiveIf_true_assert obj = new ContradictiveIf_true_assert();
    obj.c = (int) (Math.random() * 1000);

    if (a > obj.c) {
      if (a <= obj.c) {
        assert false;
      } 
    }

  }
}
