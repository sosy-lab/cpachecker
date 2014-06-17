public class Loop7 {

  public static void main(
      String[] args) {

        for(int n1 = 10 ; n1 < 20 ; n1++){

            assert(n1 > 9); // always true

            n1 = n1 + 2;
            assert(n1 < 25); // always true, n1 always lower than 23
            assert(n1 != 14); //always true, n1's values are as follows: 10, 12, 13, 15, 16, 18, 19, 21
        }
    }

}
