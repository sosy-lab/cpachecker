package pack;

public class SwitchAnweisung {

  public static void main(String[] args) {

    int condition = 1;
    int startSwitch;
    switch (condition) {
      case 0:
        int casePath0 = 0;
        break;
      case 1:
        int casePath1 = 1;
      case 2:
        int casePath2 = 2;
        break;
      default:
        int defaultPath = 3;
    }
    int endSwitch = 4;
  }
}
