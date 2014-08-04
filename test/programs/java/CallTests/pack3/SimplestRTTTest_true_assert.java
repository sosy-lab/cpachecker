package pack3;

public class SimplestRTTTest_true_assert {


  public static void main(
      String[] args) {

    SuperType1 obj2 = new SubType1(3 , 3);

    assert obj2 instanceof SubType1;


  }

}