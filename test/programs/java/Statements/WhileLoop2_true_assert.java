
public class WhileLoop2_true_assert {

  public static void main(
      String[] args) {

        int n1;

        n1 = 10;

        while(  n1 < 2 ){
          // never entered
          assert (false);
        }

 }

}
