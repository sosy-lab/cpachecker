/* This test should check usages under the lock. There was a bug, when two usages under the lock are considered as unsafe
 */
int unsafe;
int global;
int threadDispatchLevel;

int f() 
{ 
  int ret ;

  {
  threadDispatchLevel = 1;
  if (ret == 0) {
    threadDispatchLevel = 0;
    return (28);
  } 
  unsafe = 0;
  while (1) {
    if (unsafe) {
      break;
    }
    unsafe = unsafe + 1;
  }
  threadDispatchLevel = 0;
  return (0);
}
}

int ldv_main() {
    int t;
	f();
}

