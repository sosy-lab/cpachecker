public class Casts {
  
  public static void main(String[] args) {
    int    n1 = (int)   1.00001;
    byte   n2 = (byte) 257; // n4 = 1
    
    assert n1 == 1;
    assert n2 == 1;
  }
}
