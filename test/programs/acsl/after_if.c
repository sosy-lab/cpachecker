extern int __VERIFIER_nondet_int(void);

int main() {
  int a;
  while (__VERIFIER_nondet_int()) {
    if (a == 20) {
      a = 10;
    }
    //@ assert a != 20;
  }
  if (a < 10) ERROR: return 1;
  return 0;
}
