extern int __VERIFIER_assume(int);

void __VERIFIER_assert(int cond) {
  if (!cond) {
ERROR:
    return;
  }
}

int main(void) {
  int i;
  __VERIFIER_assume(i == 5);

  __VERIFIER_assert(i % 2 == 1);
  __VERIFIER_assert(-i % 2 == -1);
  __VERIFIER_assert(i % -2 == 1);
  __VERIFIER_assert(-i % -2 == -1);
}
