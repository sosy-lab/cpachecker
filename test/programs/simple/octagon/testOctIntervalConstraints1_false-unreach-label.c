void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

int main(void) {
  int a = __VERIFIER_nondet_uint();

  __VERIFIER_assert(a > 0); 

  return a;
}

