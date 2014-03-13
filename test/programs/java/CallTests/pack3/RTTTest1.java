package pack3;

public class RTTTest1 {


  public static void main(
      String[] args) {

    SubType1 obj1 = new SubType1();
    SubType1 obj2 = new SubSubType1();
    Interface2 obj3 = new SubType2(obj1);
    Interface2 obj4 = new SubType2(obj2);

    assert !obj3.objectInstanceOf();
    assert obj4.objectInstanceOf();



  }

}