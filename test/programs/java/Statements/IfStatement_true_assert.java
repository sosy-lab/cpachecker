
public class IfStatement_true_assert {


  public static void main(String[] args) {
    boolean d = true;
    boolean c = false;

    if (d || c) {
      int n = 1;

    } else {
      assert false; // not reached
    }
  }
}

