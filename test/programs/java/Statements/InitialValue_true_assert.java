
public class InitialValue_true_assert {

  public static int intValue;
  public static float floatValue;
  public static InitialValue_true_assert objectValue;
  public static int[] arrayValue;
  public static SimpleEnum enumValue;

  public enum SimpleEnum { ONE };

  public static void main(String[] args) {

    assert intValue == 0;
    assert floatValue == 0;
    assert objectValue == null;
    assert arrayValue == null;
    assert enumValue == null;
  }
}
