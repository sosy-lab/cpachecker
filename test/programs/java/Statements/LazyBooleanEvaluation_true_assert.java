
public class LazyBooleanEvaluation_true_assert {

  public static void main(String[] args) {
    checkLazyOr();
    checkLazyAnd();
  }

  private static void checkLazyOr() {
    int n = 1; 
    boolean b = true || n++ == 1;

    assert n == 1;
  }

  private static void checkLazyAnd() {
    int n = 1;
    boolean b = false && n++ != 1; 

    assert n == 1;
  }
}
