
public class StaticInitializer_true_assert {

  private static int n;

  static {
    n = 10;
  }

  public static void main(String[] args) {
    assert n == 10;
  }
}
