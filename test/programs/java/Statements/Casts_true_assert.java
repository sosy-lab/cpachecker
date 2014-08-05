public class Casts_true_assert {
  
  public static void main(String[] args) {
    int    n1 = (int)   1.00001;
    byte   n2 = (byte) 257; // n2 = 1
    
    assert n1 == 1; //always true
    assert n2 == 1; // always true
  }
}
