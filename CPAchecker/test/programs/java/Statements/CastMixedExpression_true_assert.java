public class CastMixedExpression_true_assert {

  public static void main(String[] args) {
    double a = 1.000001;
    long b = 1000;

    assert b > a; // always true
  }
}
