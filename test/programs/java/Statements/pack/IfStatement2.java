package pack;

  public class IfStatement2 {

    public static void main(
        String[] args) {

         int n1 = 1 + 1 * 4; // n1 = 5
         int n2 = 2 + 2 * 6; // n2 = 14
         boolean b1 = n1 == n2; // b1 = false

         if (b1) { // never entered
            assert(false);
          } else {

          }
      }
  }



