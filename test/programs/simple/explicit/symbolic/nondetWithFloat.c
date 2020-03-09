extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();
  float b = 2.3;

  if (a < 0) {
    a = a + 1;
    if (a < 0) {
      a = -a;
    }
  }

  b += a;

  if (b < 1) {
ERROR:
    return -1;
  }
}
