
public class IfStatement4 {




  public static void main(
      String[] args) {


        int n1 = 1;
        int n2 = 1;
        int n3 = 2;

        if(n1 == n2){

            if(n1 != n3){
                n3 = 1;
            }

            if(n1 == n3){
                n1 = n1 + n2 + n3;
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


    }
}
