
public class FunctionCall4_true_assert {

  private static int n4 = 3;
  private static int n2 = 2;
  private static int n3 = 2;

  public static void main(String[] args) {
    int n1 = 1;
    int n2 = 1; // shadows member n2

    n3 = 2;

    des(); // assert not reached

    if (n1 == n2) {
      if (n1 != n3) {
        n3 = 1;
      }

      if (n1 == n3) {
        n1 = n1 + n2 + n3; // n1 = 3
        des(); // assert not reached

      } else {
        assert(false); // assert not reached
      }

      if (n1 == n1 + n2) {
        assert(false); // assert not reached

      } else if(n1 == 2 * n2 + n3) {
        if ((n3 != n2))
          assert(false); // assert not reached
      }
    }

    des(); // assert not reached
  }


  public static void des() {
    if (n4 == n2) {
      assert(false);
    }
  }
}
