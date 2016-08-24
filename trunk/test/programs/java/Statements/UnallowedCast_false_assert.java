
public class UnallowedCast_false_assert {

  public static void main(String[] args) {
    checkInDeclaration();
    checkInAssignment();
  }

  private static void checkInDeclaration() {
    char n = 65536; // not allowed without explicit cast

    assert n == 65536;
  }

  private static void checkInAssignment() {
    char n;
    
    n = 65536;
    assert n == 65536;
  }
}
