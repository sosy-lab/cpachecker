public class BooleanOperators_true_assert {

  public static void main(String[] args) {
   boolean a = true;

   a = !a;
   
   assert a == false; 

   a |= true;
   a = a & true;
   a = a ^ false;

   assert a == true;
  }
}
