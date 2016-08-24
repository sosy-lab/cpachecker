package pack1;


import pack1.pack2.Function;
import pack3.Function2;

public class FunctionCall_true_assert {



  public static void main(
      String[] args) {

    int n = 32;

    n = Function.teileDurch2(n); // = 16
    n = Function2.teileDurch2(n); // = 8
    n = Function3.teileDurch2(n); // = 4
    assert(n == Function3.getValue()); // always true
    n = teile(n ,n); // 4 / 4 = 1
    Function3.setValue(1);
    assert(n == Function3.getValue()); // always true

  }

  public static int teile(int op , int op2) {
    return op / op2;
  }

  public static int teileRec(int op , int op2) {
    int n2 = 4;

    int n6 = 55;

    return  teile(op , op2);
  }

}
