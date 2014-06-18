
public class Assert1 {

  public static void main(
      String[] args) {

      boolean b1 = false;

      int n1 = 0;
      int n2 = 0;
      int n3 = 0;

      if((n1 == n2 && n2 == n3) || n3 == n1) {
          b1 = true;
      }

        assert b1  : "Wrong"; // b1 always true



    }
}
