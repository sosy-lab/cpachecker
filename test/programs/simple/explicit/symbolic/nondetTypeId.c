extern __VERIFIER_nondet_int();

int main() {
  int a = __VERIFIER_nondet_int();

  int b = sizeof(int);

  if (a < 0) {
    a++;
    if (a < 0) {
      a = -a;
    }
  }

  a = a + b;

  if (a < sizeof(int)) {
ERROR:
    return -1;
  }
}
