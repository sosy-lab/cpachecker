extern int __VERIFIER_nondet_int(void);

int main() {
  int a = 0;
  while (__VERIFIER_nondet_int()) {
    do {
      a++;
      //@ assert a <= 20;
    } while (a != 20);
  }
  if (a < 10) ERROR: return 1;
  return 0;
}
