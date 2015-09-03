void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

extern unsigned int __VERIFIER_nondet_uint();

int main(void) {
  unsigned int a = __VERIFIER_nondet_uint();

  __VERIFIER_assert(a > 0); 

  return a;
}

