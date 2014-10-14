
public class Loop4_false_assert {

  /*
   * Will always throw an AssertionError, if assertions allowed
   */
  public static void main(String[] args) {
    int n1;

    n1 = 10;

    do {
      assert false; // happens always
    } while (false);

  }

}
