package pack1;


import pack1.pack2.Object2;
import pack3.Object3;

public class CreateObjects {


  private int  num;
  private int  num2;

  public CreateObjects( int n1 , int n2){

    num = n1;
    num2 = n2;
 }

  public CreateObjects( int n){

    num = n;
    num2 = n;

 }

  public CreateObjects(){

    num = 0;
    num2 = 0;

 }



  private boolean  compare() {
    return (num == num2);

  }

  public static void main(
      String[] args) {



    CreateObjects object1 = new CreateObjects(2);

    CreateObjects object3 = new CreateObjects();
    CreateObjects object2 = new CreateObjects( 2 , 7);
    Object2 object4 = new Object2();
    Object2 object42 = new Object2();
    Object3 object5 = new Object3(2 ,3 ,5 , "bam");
    Object1 object6 = object5.createAnotherObject(object42);


    assert object1.compare();
    assert object3.compare();
    assert !object2.compare();


    assert  object4.getId() == 0;
    assert  object6.getId() == 1;
  }

}