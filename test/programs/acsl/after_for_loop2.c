extern int __VERIFIER_nondet_int(void);

int main() {
  int a = 0;
  while (__VERIFIER_nondet_int()) {
    int b = 0;
    for (int i = 0; i < 20; i++) {
      b++;
    }
    //@ assert b == 20;
  }
  if (a != 10) ERROR: return 1;
  return 0;
}
