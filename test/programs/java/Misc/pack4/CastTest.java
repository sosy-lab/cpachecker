package pack4;

public class CastTest {


  public static void main(String[] args) {

  SuperType1 sub = new SubType1();

  SubType1 sub2 = (SubType1) sub;

  sub.num = 2;

  sub2.num = 1;

  assert sub.num == 2;

  assert ((SubType1)sub).num == 1;

  ((SubType1) sub).method2();

  assert sub2.num == 3;

  }

}
