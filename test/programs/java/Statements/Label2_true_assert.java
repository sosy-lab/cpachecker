
public class Label2_true_assert {




  public static void main(
      String[] args) {


        int n1;

        n1 = 10;


         L1 : {


           L2 : {

             L3 : {

               break L2;

               assert false; // not reached
             }

             assert false; // not reached
           }

           L4: {
             break L4;
             assert false; // not reached
           }
         }
      }
}
