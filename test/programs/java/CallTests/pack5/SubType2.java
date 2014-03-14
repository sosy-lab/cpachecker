package pack5;

public class SubType2 extends SuperType1 implements Interface2{


  int subNum1;
  int subNum2;
  SubType1 sub1 = null;


  public SubType2() {

    super();
    subNum1 = 0;
    subNum2 = 0;
    num1 = 0;
    num2 = 0;
    sub1 = new SubType1();

  }

  public SubType2(int num1 , int num2){

    super();
    subNum1 = num1;
    subNum2 = num2;
    super.num1 = num1 + num2;
    super.num2 = num1 + num2;
    sub1 = new SubType1(num1, num2);
  }

  public SubType2(SubType1 sss){
    super();
    sub1 = sss;
    super.num1 = sss.add();

    //System.out.println(super.num1);

    assert super.num1 == 43 || super.num1 == 8;
    super.num2 = sss.addAll();
    subNum1 = sss.num1;
    subNum2 = sss.num2;
  }

  public int getNum1(){
    return subNum1;
  }

  public int getNum2(){
    return subNum2;
  }

  public SubType1 getSub1() {
  return sub1;
  }

  public void setSub1(SubType1 sub1) {
  this.sub1 = sub1;
  }

  public SubType2 construct() {

  SubType2 a = new SubType2(sub1);

  //System.out.println(a.num2);

  assert a.num2 == 23 || a.num2 == 548;

  return new SubType2(sub1);
  }

  public int test() {
  int numx = 0;


  assert subNum1 == 7;
  assert subNum2 == 8;
  assert super.num1 == 8;
  assert super.num2 == 23;

    numx = construct().getSub1().num2 % (subNum2 + 11);

  assert numx == 8;


  numx = numx / sub1.addAll() + sub1.addAll();

  assert numx == 23;

    return (numx * construct().sub1.add() << 1);
  }

  public boolean objectInstanceOf(){
    boolean b = sub1 instanceof SubSubType1;
    return b;
  }

}