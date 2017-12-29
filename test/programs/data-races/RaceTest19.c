/* This test is similar to RaceTest18.c, but it has two usages with two locks
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
}

