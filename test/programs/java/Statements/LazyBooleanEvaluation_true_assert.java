
public class LazyBooleanEvaluation_true_assert {

  public static void main(String[] args) {
    final boolean t = true;
    final boolean f = false;
    boolean tmp;

    if (t || provokeAssertionError()) { }
    if (f && provokeAssertionError()) { }

    tmp = t || provokeAssertionError();
    tmp = f && provokeAssertionError();

    tmp = (t || provokeAssertionError()) ? (t || provokeAssertionError()) : false;
    tmp = (f && provokeAssertionError()) ? true : (f && provokeAssertionError());

    if (t != (f && provokeAssertionError())) { }
    if (f != (t || provokeAssertionError())) { }
    
    tmp = t != (f && provokeAssertionError());
    tmp = f == (t || provokeAssertionError());

    if (!!!(t || provokeAssertionError())) { }
    if (!!!(f && provokeAssertionError())) { }

    int test_multiple_operators_not_nested;
    int n = 0;
    tmp = (t || provokeAssertionError()) | (f && provokeAssertionError()) | n++ == 0;
    assert n == 1;    

    int test_expression_list_statement;
    boolean __t;
    boolean __f;
    tmp =  (__t = t) == true
      && !(__f = f) == false
      && (f && provokeAssertionError()) ? true : (f && provokeAssertionError());
  }

  private static boolean provokeAssertionError() {
    assert false;
    return true;
  }
}
