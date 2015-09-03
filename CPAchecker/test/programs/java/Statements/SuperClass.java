
public class SuperClass {

  public static void main(String[] args) {
    boolean b = true;
    SuperClass obj;

    if (b) {
      obj = new SuperClass();
    } else {
      obj = new SubClass();
    }

    int startMethodInvocation;
    obj.method();
    int endMethodInvocation;
  }

  public void method() {
    int startMethod;
    int endMethod;
  }
}
