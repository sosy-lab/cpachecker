package pack5;

public class SwitchTest_true_assert {

  public static void main(String[] args) {
    WLE s = WLE.JA;

    switch(s){
    case JA:
      assert true; // always true
      break;
    case NEIN:
      assert false; // not reached
    }
  }

  public enum WLE {
    JA,
    NEIN;
  }
}
