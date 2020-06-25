extern __VERIFIER_nondet_int();

int main() {
  int a;
  int b;

  a = __VERIFIER_nondet_int();
  b = __VERIFIER_nondet_int();

  if (a == b) {
    goto ERROR;
  } else {
    a = b;
  }

  if (a == b) {
    return 0;
  } else {
ERROR:
    return -1;
  }
}
