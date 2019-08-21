package pack1;

import pack1.pack2.Object2;
import pack3.Object3;

public class CreateObjects_true_assert {

  private int  num;
  private int  num2;

  public CreateObjects_true_assert(int n1 , int n2){
    num = n1;
    num2 = n2;
 }

  public CreateObjects_true_assert(int n){
    num = n;
    num2 = n;
 }

  public CreateObjects_true_assert(){
    num = 0;
    num2 = 0;
 }



  private boolean compare() {
    return (num == num2);

  }

  public static void main(String[] args) {
    CreateObjects_true_assert object1 = new CreateObjects_true_assert(2);

    CreateObjects_true_assert object3 = new CreateObjects_true_assert();
    CreateObjects_true_assert object2 = new CreateObjects_true_assert(2, 7);
    Object2 object4 = new Object2();
    Object2 object42 = new Object2();
    Object3 object5 = new Object3(2, 3, 5, "bam");
    Object1 object6 = object5.createAnotherObject(object42);

    assert object1.compare(); // 2 == 2, always true
    assert object3.compare(); // 0 == 0, always true
    assert !object2.compare(); // !(2 == 7), always true

    assert object4.getId() == 0; // 0 == 0, always true
    assert object6.getId() == 1; // always true
  }
}

