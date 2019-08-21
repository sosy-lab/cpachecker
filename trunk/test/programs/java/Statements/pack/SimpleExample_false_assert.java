package pack;

public class SimpleExample_false_assert {

  public static void main(String[] args) {

    int n1 = 3 + 3 * 4; // n1 = 15
    int n2 = 2 + 2 * 6; // n2 = 14
    boolean b1 = n1 == n2; // b1 = false

    assert b1; // always false
  }
}
