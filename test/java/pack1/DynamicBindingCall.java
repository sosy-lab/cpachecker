package pack1; 


import pack3.*;

public class DynamicBindingCall  {
  
  public static void main(String[] args){
        
    SuperType1 obj3 = new SuperType1(1 , 2);
    SuperType1 obj1 = null;
    
    int n = 2;
    int n2 = 4;
    
    if(n == n2) {
      obj1 = new SubType1(1 ,1);
    } else {
      obj1 = new SubSubType1( 1 , 2);
    }
        
    obj1.add();
    SuperType1 obj2 = new SubType1(3 , 3);
    assert obj2.co();    
  }
}