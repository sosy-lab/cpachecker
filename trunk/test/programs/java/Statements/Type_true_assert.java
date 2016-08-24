public class Type_true_assert {

  int field1;

  public Type_true_assert(int param) {
    field1 = param;
  }

  public void increment() {
    field1 = field1 + 1;
  }

  public int getField() {
    return field1;
  }

  public static void main(String[] args) {
    Type_true_assert type = null;
    int y = 2;

    switch (args.length) {
      // both branches possible
      case 1:
        y = 6;
        break;
      case 2:
        y = 4;
    }

    for (int i = 1; i < y; i++) { // 1, 3 or 5 iterations
      if (i > y / 2) {
        type.increment();
      }

      if (i == y / 2) {
        type = new Type_true_assert(1);
      }

      assert i < y; // always true
    }

    assert y / 2 == type.getField(); // always true with above loop
  }
}
