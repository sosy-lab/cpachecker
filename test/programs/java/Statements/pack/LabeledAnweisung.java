package pack;

public class LabeledAnweisung {

  public static void main(String[] args) {

  boolean breakCondition = true;
  int startLabel;

  Label: {
    int startLabelBlock;

    if (breakCondition) {
    break Label;
    }

    int stopLabelBlock;
  }

  int endLabel;
  }
}
