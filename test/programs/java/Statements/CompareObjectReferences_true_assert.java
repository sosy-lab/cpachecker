
public class CompareObjectReferences_true_assert {

  public static void main(String[] args) {
    CompareObjectReferences a = new CompareObjectReferences();
    CompareObjectReferences b = a;

    assert ( a == b ); // always true
  }
}
