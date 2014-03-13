package pack5;

public class switchTest {


  public static void main(String[] args) {


  WLE s = WLE.JA;

  switch(s){
  case JA:
    assert true;
  break;
  case NEIN:
    assert false;

  }



  }

 public enum WLE {
  JA,
  NEIN;
  }

}
