
public class FunctionCall_true_assert {

  public static void main(String[] args) {

    int n1 = 1;
    int n2 = 1;
    int n3 = 2;

    if (n1 == n2) {

      if (n1 != n3) {
        n3 = 1;
        des();
      }

      if (n1 == n3) {
        des();
        n1 = n1 + n2 + n3; // n1 = 3

      } else {
        assert false; // not reached
      }

      if (n1 == n1 + n2) {
        assert false; // not reached

      } else if (n1 == 2 * n2 + n3) {
        assert n3 == n2; // always true
      }
    }

    des();
}

  public static void des() {
    int n1 = 1;
    int n2 = 2;

    if (n1 == n2) {
      assert(false); // not reached
    }
  }
}
