public class Casts_true_assert {

  public static void main(String[] args) {
    checkFloatCast();
    checkCastWithOverflow();
    checkPrecisionLoss();
    checkFloatPrecision();
  }

  private static void checkFloatCast() {
    int n = (int) 1.00001;

    assert n == 1; //always true
  }

  private static void checkCastWithOverflow() {
    byte n = (byte) 257; // n2 = 1

    assert n == 1; // always true
  }

  private static void checkPrecisionLoss() {
    int intValue = 1234567890;
    float floatValue = intValue;

    assert intValue - (int) floatValue == -46;

    float firstValue = 125.32f;
    float sndValue = 125.32001f;
    
    assert 125.3200001f == firstValue;
    assert 125.3200099f == sndValue;

    double smallestNumberGreaterOne = 1.0000000000000002;
    double roundedUpValue           = 1.0000000000000001999;
    double roundedDownValue         = 1.00000000000000005;
    
    assert smallestNumberGreaterOne == roundedUpValue;
    assert roundedDownValue == 1;
  }


  private static void checkFloatPrecision() {
    float n1 = ((float) 8) / 7;
    double n2 = ((double) 8) / ((double) 7);
    double epsilon = 0.0001;
    
    assert n1 == 8 / ((float) 7);
    assert n2 - 1.1428571428571428 < epsilon; 
  }
}
