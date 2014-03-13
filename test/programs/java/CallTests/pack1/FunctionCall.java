package pack1;


import pack1.pack2.Function;
import pack3.Function2;

public class FunctionCall {



  public static void main(
      String[] args) {

    int n = 32;

    n = Function.teileDurch2(n);
    n = Function2.teileDurch2(n);
    n = Function3.teileDurch2(n);
    assert(n == Function3.getValue());
    n = teile(n ,n);
    Function3.setValue(1);
    assert(n == Function3.getValue());

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