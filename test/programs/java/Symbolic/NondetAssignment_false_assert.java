import java.lang.Math;

public class NondetAssignment_false_assert {
  
  public static void main(String[] args) {
    int a = (int) (Math.random() * 15);
    int b = a;
    
    a = (int) (Math.random() * 2);

    assert a == b;
  }
}
