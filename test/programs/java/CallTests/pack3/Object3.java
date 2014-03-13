package pack3;

import pack1.pack2.Object2;
import pack1.Object1;

public class Object3 {


  int num1;
  int num2;
  int num3;
  String name;



  public Object3(int num1 , int num2, int num3, String name){
    this.num1 = num1;
    this.num2 = num2;
    this.num3 = num3;
    this.name = name;
  }

  public Object1 createAnotherObject(Object2 object2){
   return  new Object1(num1 , num2 , num3 , name , object2);

  }

  public int getNum1(){
    return num1;
  }

  public int getNum2(){
    return num2;
  }

  public int getNum3(){
    return num3;
  }

  public int getName(){
    return name;
  }

}