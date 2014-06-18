public class CastMixedExpression {

  public static void main(String[] args) {
    double a = 1.000001;
    long   b = 1000;

    assert b > a;  
  }

}
