package pack5;

public class SubSubType2 extends SubType1 {


  int subsubNum1;
  int subsubNum2;


  public SubSubType2(){
    super();
    subsubNum1 = 0;
    subsubNum2 = 0;
  }


  public SubSubType2(int num1 , int num2){
    super();
    subsubNum1 = num1;
    subsubNum2 = num2;
  }

  public int getNum1(){
    return subsubNum1;
  }

  public int getNum2(){
    return subsubNum2;
  }

}