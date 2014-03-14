
public class FunctionCall5 {

  public static void main(
      String[] args) {

      int n = 32;

      n = teileDurch2(n);
      n = teileDurch2(n);
      n = teileDurch2(n);
      assert(n == 4);
      n = teile(n ,n);
      assert(n == 1);

    }


      public static int teileDurch2(int op) {
        return op / 2;
    }

    public static int teile(int op , int op2) {
      return op / op2;
    }
}
