
public class FunctionCall3 {

  private static int n1 = 1;
  private static int n2 = 1;
  private static int n3 = 2;

  public static void main(
      String[] args) {


         n1 = 1;
         n2 = 2;
         n3 = 2;



        des();

        if(n1 != n2){

            n2 = 1;

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
            } else if(n1 == n1 + n2 + n3){

            des();
            assert(n1 == n1 + n2 + n3);

            }

        } else {

        assert (false);

        }

          des();

    }


      public static void des() {

      int n1 = 1;
      int n2 = 1;

        if(n1 == n2){



        } else {

        assert(false);

        }

    }
}
