package pack1;



import pack3.*;

public class BooleanOperators  {

  public static void main(String[] args){


    int n1 = 0;

    int n2 = 1;


    if(((4 / 2) == 2 | ++n1 || ++n1 == ++n2 )  // 2 == (2 | ++n1) || ++n1 == ++n2
        && ++n1 == 0 & ++n2 == 1 || ++n1 == 3) { // (++n1 == 0 & ++n2 == 1) || ++n1 == 3

      assert n1 == n2;
      return;
    }

    assert false;

  }
}
