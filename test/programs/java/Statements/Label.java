
public class Label {




  public static void main(
      String[] args) {


        int n1;

        n1 = 10;


        l1 : {

           n1 = 5;


           break l1;

           assert false ;



        }

        n1 = 6;

        }

}
