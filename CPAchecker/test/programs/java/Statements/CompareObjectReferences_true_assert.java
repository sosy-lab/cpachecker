
public class CompareObjectReferences_true_assert {

  public static void main(String[] args) {
    CompareObjectReferences_true_assert a = new CompareObjectReferences_true_assert();
    CompareObjectReferences_true_assert b = a;

    assert (a == b); // always true
  }
}
