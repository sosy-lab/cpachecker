public class Type {

  int field1;

  public Type(int param) {
  field1 = param;
  }

  public void increment() {
  field1 = field1 + 1;
  }

  public int getField() {
  return field1;
  }

  public static void main(String[] args) {

  Type type = null;
  int y = 2;

  switch (args.length) {
  case 1:
    y = 6;
    break;
  case 2:
    y = 4;
  }

  for (int i = 1; i < y; i++) {
    if (i > y / 2) {
    type.increment();
    }

    if (i == y / 2) {
    type = new Type(1);
    }
    assert i < y;
  }
  assert y / 2 == type.getField();
  }
}