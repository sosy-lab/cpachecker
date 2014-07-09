
public class FunctionCall5_true_assert {

  public static void main(
      String[] args) {

      int n = 32;

      n = teileDurch2(n);
      n = teileDurch2(n);
      n = teileDurch2(n); // n = 4
      assert(n == 4); // always true
      n = teile(n ,n); // n = 1
      assert(n == 1); // always true

    }


    public static int teileDurch2(int op) {
        return op / 2;
    }

    public static int teile(int op , int op2) {
      return op / op2;
    }
}
