
public class WhileLoop1_true_assert {

  public static void main(
      String[] args) {

    int n1;

    n1 = 0;

    while (n1 < 10) {

      n1 = n1 + 1;

      if (n1 > 10) // never entered, n1 <= 10 always
          assert (false);
    }
  }
}
