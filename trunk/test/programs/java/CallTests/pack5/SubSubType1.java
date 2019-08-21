package pack5;

public class SubSubType1 extends SubType1 {


  int subsubNum1;
  int subsubNum2;


  public SubSubType1(int num1 , int num2){
    super();
    subsubNum1 = num1;
    subsubNum2 = num2;
  }

  public SubSubType1(){
    super();
    subsubNum1 = 0;
    subsubNum2 = 0;
  }

  public int getNum1(){
    return subsubNum1;
  }

  public int getNum2(){
    return subsubNum2;
  }

  public int add(){
    return subsubNum1 + subsubNum2;
  }

  public boolean compare(){
    return (subsubNum1 == subsubNum2);
  }

}