package pack1;



import pack3.*;

/* Comments in this class are meant for faster understanding only
 * and may be wrong if someone changed the file without changing the comments
 * appropriately. So be careful.
 */
public class DynamicBindingCall_true_assert  {

  public static void main(String[] args){

    SuperType1 obj1 = new SuperType1(1 , 2);

    int n = 2;
    int n2 = 4;

    if(n == n2) {
      obj1 = new SubType1(1 ,1);
    } else {
      // enters this branch always
      obj1 = new SubSubType1( 1 , 2);
    }

    obj1.add(); // does nothing
    Interface1 obj2 = null;

    if(n == n2){
     obj2 = new SubSubType1(3 , 3);
    }else {
    // enters this branch always
     obj2 = new SubType1(3 , 3 , 4 , 4);
    }

    boolean b11 = false;
    if( obj2 instanceof SubType1) {
      // enters this branch, independently of the above assignments
      b11 = true;
    }

    assert b11; // b11 always true

    boolean b10 = obj2 instanceof Interface1; // always true

    boolean b1 = obj2.compare(); // returns 3 == 3 && 4 == 4, always true

    assert b1; // b1 always true

    Interface1 obj3 = null;

    obj2 = new SubSubType1(3 , 3);

    obj3 = obj2;

    b1 = obj3.compare(); // returns 3 == 3, always true
    assert b1; // always true
  }
}
