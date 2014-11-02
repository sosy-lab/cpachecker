
public class UnallowedCast_false_assert {

  public static void main(String[] args) {
    char n = 65536; // not allowed without explicit cast

    assert n == 65536;
  }
}
