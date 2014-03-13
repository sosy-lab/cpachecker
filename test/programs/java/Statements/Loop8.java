public class Loop8 {




  public static void main(
       String[] args) {


        int n1;

        n1 = 0;


        for(n1 = 0 ; n1 < 20 ; n1 = n1 + 1){


            if(n1 < 10){
              continue;
            }

            assert(n1 > 9);

            if(n1 == 15){
                break;
            }

            assert(n1 < 15);

        }

  }
}
