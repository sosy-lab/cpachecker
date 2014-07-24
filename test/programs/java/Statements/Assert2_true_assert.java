
public class Assert2_true_assert {


  public static void main(
      String[] args) {


        int n1;
        int n2;
        int n3;

        n1 = 9;

        n2 = 9;

        n3 = 9;

        // comparison always true
        assert (n1 == n2) && (n2 == n3) : "The Values are not equal";

        }
}
