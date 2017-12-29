/* This test should check usages under the lock. There was a bug, when two usages under the lock are considered as unsafe
 */
int unsafe;
int global;

int f() 
{ 
  intLock();
  g();
  intUnlock();
  return (0);
}

int g() {
  unsafe = 0;
}

int ldv_main() {
    int t;
    if (t) {
      splbio();
    } else {
      kernDispatchDisable();
    }
    f();
    splx();
    kernDispatchEnable();
    f();
}

