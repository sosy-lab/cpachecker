extern int __VERIFIER_nondet_int(void);

int main() {
  int a;
  while (__VERIFIER_nondet_int()) {
    a = 19;
    //@ assert a == 19;
    a++;
    if (a == 20) {
      a = 10;
    }
  }
  if (a < 10) ERROR: return 1;
  return 0;
}
