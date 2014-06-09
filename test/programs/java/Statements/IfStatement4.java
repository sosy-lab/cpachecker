
public class IfStatement4 {




  public static void main(
      String[] args) {


        int n1 = 1;
        int n2 = 1;
        int n3 = 2;

        if(n1 == n2){
            // branch entered
            if(n1 != n3){
                // branch entered
                n3 = 1;
            }

            if(n1 == n3){
                // branch entered
                n1 = n1 + n2 + n3; // n1 = 3
            }else {
                assert(false); // not reached
            }

            if(n1 == n1 + n2){
            assert(false); // not reached
            } else if(n1 == 2 * n2 + n3){
                // branch entered
            if((n3 != n2)) // n3 == n1 = true
             assert(false); // not reached
            }
        }
    }
}
