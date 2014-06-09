public class CastFloat {
  
  public static void main(String[] args) {
    float  n2 = (float) 1.00001;
    double n3 =         1.00001;
    
    assert(n2 == n3);
  }
}
