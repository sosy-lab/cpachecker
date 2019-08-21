  int a,c,d;

  int func(int b,int c) {
     if (b == 1) a = 1;
     else a = 0;
    return 2;
  }

  main() {
      a = 1;
      c = 1;
      d = func(a,c);

      // this is used as hook to check if c == 0
      //(as it should be when modified with the modifyingAutomaton)
      somefunction();

      if (c!=0) {
    	  error();
      }
  }
