package pack1;

import pack3.Object3;
import pack1.pack2.Object2;



public class Object1 {

  int num1;
  int num2;
  int num3;
  String name;
  int id;


public Object1(int pnum1, int pnum2, int pnum3, String pname, Object2 object2){
  num1 = pnum1;
  num2 = pnum2;
  num3 = pnum3;
  name = pname;
  id = object2.getId();
}

public Object1(Object2 object2, Object3 object3){

  num1 = object3.getNum1();
  num2 = object3.getNum2();
  num3 = object3.getNum3();
  name = object3.getName();
  id = object2.getId();

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

public String getName(){
  return name;
}

public int getId(){
  return id;
}

}
