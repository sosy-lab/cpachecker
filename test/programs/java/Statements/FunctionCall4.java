
public class FunctionCall4 {

  private static int n4 = 3;
  private static int n2 = 2;
  private static int n3 = 2;

  public static void main(
      String[] args) {


         int n1 = 1;
         int n2 = 1;
         n3 = 2;

        des();

        if(n1 == n2){

            if(n1 != n3){
                n3 = 1;

            }

            if(n1 == n3){
                n1 = n1 + n2 + n3;
                des();
            }else {
                assert(false);
            }

            if(n1 == n1 + n2){
            assert(false);
            } else if(n1 == 2 * n2 + n3){
            if((n3 != n2))
             assert(false);
            }
        }

          des();

    }


      public static void des() {



        if(n4 == n2){

        assert(false);

        } else {

        }

    }
}
