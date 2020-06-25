
public class ExceptionCatch_true_assert {

  public static void main(String[] args) {
    checkExceptionCatched();
    checkNoExceptionThrown();
  }

  private static void checkExceptionCatched() {
    int n = 1;

    try {
      n = throwExceptionIndirectly(); // no new value assigned
    } catch (Exception e) {
      n = 2;
    } finally {
      assert n == 2;

      n = 3;
    }

    assert n == 3;
  }

  private static int throwExceptionIndirectly() throws Exception {
    throwException();
    return 10;
  }

  private static int throwException() throws Exception {
    throw new Exception();
  }


  private static void checkNoExceptionThrown() {
    int n = 1;

    try {
      n = getValue(true);
    } catch (Exception e) {
      n = 2;
    } finally {
      assert n == 1;

      n = 3;
    }

    assert n == 3;
  }

  private static int getValue(boolean getValue) throws Exception {
    if (getValue) {
      return 1;
    } else {
      throw new Exception();
    }
  }
}
