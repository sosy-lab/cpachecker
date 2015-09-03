package pack4;

public class SubType1_true_assert extends SuperType1 {

  public int num = 0;
  public SuperSuper sup;
  public SuperSuperSuper sub;

  public SubType1_true_assert() {
    super();

    sup = new SuperSuper();
    sub = new SuperSuperSuper();
  }

  public static void main(String[] args) {
    SubType1_true_assert s = new SubType1_true_assert();

    s.method();
  }

  public void method2() {
    num = 3;
  }

  @Override
  public void method() {
    super.method(); // super.num = 5

    int num = 1; // shadows member num

    SuperSuper sup = new SuperSuper();

    this.num = 2;

    sup.num = 3;

    this.sup.num = 4;

    super.sup.num = 5;

    sup.sub.method2();

    super.sup.sub.method2();

    super.sup.sub.num = 6;

    assert num == 1; // local num; always true

    assert this.num == 2; // always true

    assert sup.num == 3; // local sup; always true

    assert this.sup.num == 4; // always true

    assert super.sup.num == 5; // always true

    assert super.sup.sub.num == 6; // always true
  }
}
