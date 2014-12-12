import util.SimpleEnum;

public class Arrays_true_assert {
  
  public static void main(String[] args) {
    checkInitialValues();
    checkCallByReference();
  }

  private static void checkInitialValues() {
    int[] intArray = new int[5];
    double[] doubleArray = new double[2];
    boolean[] boolArray = new boolean[10];
    Object[] objectArray = new Object[10];
    SimpleEnum[] enumArray = new SimpleEnum[5];
    
    assert intArray[3] == 0;
    assert doubleArray[0] == 0;
    assert boolArray[1] == false;
    assert objectArray[9] == null;
    assert enumArray[4] == null;
  }

  private static void checkCallByReference() {
    int[][] numberArray = { { 1 }, { 2, 3 } };
    int[] subArray = numberArray[0];

    assert numberArray[1][1] == 3;
    assert subArray[0] == 1;

    subArray[0] = 5;
    assert numberArray[0][0] == 5;
    
    subArray = new int[5];
    subArray[0] = 1;
    assert numberArray[0][0] == 5;
    assert subArray[0] == 1;

    boolean[][][] booleanArray = new boolean[100][50][20];
    booleanArray[90][40][1] = true;
    assert booleanArray[90][40][1] == true;
  }
}
