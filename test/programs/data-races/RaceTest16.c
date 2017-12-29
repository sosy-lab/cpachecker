/* This test should check, how the tool handle abstractions, which are false without any predicates
 */
int unsafe;
int global;

int f(int t ) 
{ 
  int oldflags ;
  int s ;
  if (oldflags > 36) {
    global = 1;
    if (oldflags == 9) {
        func(0);
    }
    if (t == 40) {
        func(t);
    }
  } 
return (0);
}

int func(int p) {
    if (p == 0) {
      unsafe = 1;
    }
    global = 0;
    return 0;
}

int ldv_main() {
    int t;
	f(40);
}

