
public class Loop3 {

  public static void main(String[] args) {
        int n1;
        int n2;
        int n3;

        n1 = 0;
        n2 = 0;
        n3 = 0;

        while(  n1 < 10  ){

            if (n1 == 3){

                while (n2 < 4){

                    if(n2 == 3){

                        while (n3 < 6){
    
                            if(n3 == 3){
                                assert(n3 == 3); // always true
                                assert(n1 == 3); // always true, n1 = n3 as long as n1 <= 6
                                assert(n2 == 3); // always true, n1 = n2 as long as n2 <= 4
                             }
                            n3 = n3 + 1;
                        }
                    }

                    n2 = n2 + 1;
                }
            }

            n1 = n1 + 1;
        }

        assert(n1 == 10); // always true
        assert(n2 == 4); // always true
        assert(n3 == 6); // always true
    }
}
