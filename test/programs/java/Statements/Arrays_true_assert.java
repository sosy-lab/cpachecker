
public class Arrays_true_assert {
  
  public static void main(String[] args) {
    int[][] numberArray = { { 1 }, { 2, 3 } };
    int[] subArray = numberArray[0];

    assert numberArray[1][1] == 3;
    assert subArray[0] == 1;
  }
}
