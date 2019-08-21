package pack3;

public class SubType1 extends SuperType1 implements Interface1 {


  int subNum1;
  int subNum2;

  public SubType1(){

    super();
    subNum1 = 0;
    subNum2 = 0;
  }


  public SubType1(int num1 , int num2){

    super();
    subNum1 = num1;
    subNum2 = num2;
  }

  public SubType1(int num1 , int num2, int superNum1 , int superNum2){

    super(superNum1, superNum2);
    subNum1 = num1;
    subNum2 = num2;
  }



  public int getNum1(){
    return subNum1;
  }

  public int getNum2(){
    return subNum2;
  }

  @Override
  public boolean  compare() {

    boolean c;
    c = subNum1 == subNum2 && num == num2;


    return   c;
  }

  public int add(){
    return subNum1 + subNum2;
  }
}