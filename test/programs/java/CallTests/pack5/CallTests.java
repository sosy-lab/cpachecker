package pack5;

public class CallTests {

  public static void main(String[] args) {

  SubType2 su = new SubType2(new SubType1(3, 5, 7, 8));

   int t = su.test();

   assert t == 368;

  }

}
