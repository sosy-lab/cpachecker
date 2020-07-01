package pack3;

public class SubType2 extends SuperType1 implements Interface2{


  int subNum1;
  int subNum2;
  SubType1 sub1 = null;


  public SubType2() {

    super();
    subNum1 = 0;
    subNum2 = 0;
  }



  public SubType2(int num1 , int num2){

    super();
    subNum1 = num1;
    subNum2 = num2;
  }

  public SubType2(SubType1 num1){

    super();
    sub1 = num1;
  }

  public int getNum1(){
    return subNum1;
  }

  public int getNum2(){
    return subNum2;
  }

  public boolean objectInstanceOf(){
    boolean b = sub1 instanceof SubSubType1;
    return b;
  }

}