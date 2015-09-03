
public class ForEach_true_assert {

  public static void main(String[] args) {
    int[] numberArray = { 1, 2, 3, 4, 5 };
    int counter = 0;

    for (int currentNumber : numberArray) {
      assert numberArray[counter] == currentNumber;
      counter = counter + 1;
    }
  }
}
