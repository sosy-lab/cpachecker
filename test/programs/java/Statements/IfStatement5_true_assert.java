
public class IfStatement5_true_assert {


  public static void main(String[] args) {
    boolean a1 = true;
    boolean a2 = false;
    boolean a3 = false;
    boolean a4 = true;
    boolean a5 = (a4 && a1) || a3; // a4 && a1 = true


    if (((a1 && !a2) || !(a3 && a4)) && a5) { // a1 && !a2 && a5 = true

    } else {
      assert false; // never reached
    }
  }
}
