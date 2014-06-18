
public class ConditionalExpression1 {


  public static void main(
      String[] args) {


        int n1;
        int n2;

        n1 = 9;

        n2 = 10;

        n1 = n1 == n2 ? n1 : n2; // n1 = n2

        assert n1 == n2 : "The Values are not equal"; // always true

        }
}
