
public class InnerClass_true_assert {

  public static void main(String[] args) {
    checkStaticInnerClass();
    checkDynamicInnerClass();
  }

// Static classes are tested in this part //

  private static class Pair {
    int first;
    int second;
  }

  // checks explicit and implicit statement of "outer class" and access to the static inner class
  private static void checkStaticInnerClass() {
    Pair p = new InnerClass_true_assert.Pair();
    p.first = 5;

    assert p.first == 5;

    p = new Pair();
    p.first = 1;
    
    assert p.first == 1;
  }

// Dynamic inner classes are tested in this part //
 
  private static InnerClass_true_assert outerClassObject = new InnerClass_true_assert();

  private int key = 100;
  private int unchangedField = 2;

  private class Identifier {
    int key = 1; // shadows field "key" of outer class

    void incrementOuterClassKey() {
      InnerClass_true_assert.this.key++;
    }

    void checkAccessToOuterClass() {
      assert key == 1;
      assert InnerClass_true_assert.this.unchangedField == 2;
    }
  }

  private static void checkDynamicInnerClass() {

    checkAccessFromOutside();
    checkAccessFromInside();
  }

  private static void checkAccessFromOutside() {
    
    Identifier i = outerClassObject.new Identifier();
    
    i.key = 5;
    assert i.key == 5;
    assert outerClassObject.key == 100;
  }

  private static void checkAccessFromInside() {
    Identifier i = outerClassObject.new Identifier();

    i.incrementOuterClassKey();
    assert outerClassObject.key == 101;

    i.checkAccessToOuterClass();
  }
}
