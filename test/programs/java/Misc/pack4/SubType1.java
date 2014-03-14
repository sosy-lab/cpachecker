package pack4;

public class SubType1 extends SuperType1 {

  public int num = 0;
  public SuperSuper sup;
  public SuperSuperSuper sub;

  public SubType1() {

  super();

  sup = new SuperSuper();
  sub = new SuperSuperSuper();

  }

  public static void main(String[] args) {

  SubType1 s = new SubType1();

  s.method();

  }

  public void method2() {
  num = 3;
  }

  public void method() {

  super.method();

  int num = 1;

  SuperSuper sup = new SuperSuper();

  this.num = 2;

  sup.num = 3;

  this.sup.num = 4;

  super.sup.num = 5;

  sup.sub.method2();

  super.sup.sub.method2();

  super.sup.sub.num = 6;

  assert num == 1;

  assert this.num == 2;

  assert sup.num == 3;

  assert this.sup.num == 4;

  assert super.sup.num == 5;

  assert super.sup.sub.num == 6;

  }
}