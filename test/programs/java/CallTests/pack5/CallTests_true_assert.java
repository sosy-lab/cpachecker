package pack5;

public class CallTests_true_assert {

  public static void main(String[] args) {

  SubType2 su = new SubType2(new SubType1(3, 5, 7, 8)); // no assert violated

   int t = su.test(); // no assert violated

   assert t == 368; // always true
  }

}
