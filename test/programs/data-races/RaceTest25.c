/* This test should check, how the tool handle abort functions (we can not find unsafes in and after these functions)
 */
int unsafe;
int global;

void abort() 
{ 
  int oldflags ;
  int s ;
  if (oldflags > 36) {
    unsafe = 1;
  } 
}

int func(int p) {
    abort();
    global = 0;
    return 0;
}

int ldv_main() {
    int t;
	func(40);
}

