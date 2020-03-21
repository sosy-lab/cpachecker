extern int __VERIFIER_nondet_uint();
extern void __VERIFIER_error();

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

int main() {
  int n = 0;
  for (int i = 0; i<5; i++) {
    if (__VERIFIER_nondet_uint() % 2 == 0) {
      n = 1;
      break;
    };
  }
  __VERIFIER_assert(n == 1);
  return 0;
}
