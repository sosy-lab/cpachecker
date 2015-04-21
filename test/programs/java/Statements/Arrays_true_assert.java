import util.SimpleEnum;

public class Arrays_true_assert {
  
  public static void main(String[] args) {
    checkInitialValues();
    checkCallByReference();
    checkLength(args);
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

  private static void checkLength(String[] args) {
    int[][] intArray = new int[10][5];

    assert intArray.length == 10;
    assert intArray[0].length == 5;

    intArray[5] = new int[100];

    assert intArray[5].length == 100;

    checkLength(intArray);

    switch (args.length) {
      case 0:
      case 1:
      case 2:
        assert true;
      default:
        // DO NOTHING
    }

    if (args.length == 0) {
      if (args.length >= 0) {
        assert args.length == 0;
      }
    }
  }

  private static void checkLength(int[][] array) {
    assert array.length == 10;
    assert array[8].length == 5;
  }
}
