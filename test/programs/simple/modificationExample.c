  int along,clong,d;

  int func(int b,int c) {
     if (b == 1) along = 1;
     else along = 0;
    return 2;
  }

  main() {
      along = 1;
      clong = 1;
      d = func(along,clong);

      // this is used as hook to check if c == 0
      //(as it should be when modified with the modifyingAutomaton)
      somefunction();

      if (clong!=0) {
    	  error();
      }
  }
