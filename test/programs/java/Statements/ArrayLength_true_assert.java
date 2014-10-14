public class ArrayLength_true_assert {

  public static void main(String[] args) {
    int arrayLength = 5;
    int[] a = new int[arrayLength];

    assert a.length == arrayLength;
  }
}
