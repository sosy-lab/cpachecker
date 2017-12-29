/* There is a bug with reading under the locks - all locks are lost
 */
int unsafe;
int unsafe2;

int f() 
{ 
  intLock();
  unsafe = 1;
  unsafe2 = 1;
  intUnlock();
}

int g() {
  int tmp;
  intLock();
  tmp = unsafe;
  intUnlock();
  tmp = unsafe2;
}

int ldv_main() {
    f();
    g();
}

