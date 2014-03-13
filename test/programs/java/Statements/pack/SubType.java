package pack;

public class SubType extends SuperType{

  int subField;

  public SubType(int superParam , int subParam) {
    super(superParam);
    int startSubConstructor;
    subField = subParam;
    int endSubConstructor;
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    SuperType subType = new SubType(1 ,1);
  }

}
