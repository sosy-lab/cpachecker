extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();;
  int b = 5;

  while (1) {
    a = a + __VERIFIER_nondet_int();

    if (a > 0) {
      b = 10;
    }

    if (b == 20) {
ERROR:
      return -1;
    }
  }
}
