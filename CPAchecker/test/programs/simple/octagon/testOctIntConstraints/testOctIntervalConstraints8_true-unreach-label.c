void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

extern unsigned int __VERIFIER_nondet_uint();

int main(void) {
  unsigned int a = __VERIFIER_nondet_uint(); //interval from 0 to infinity

  if (a > 0) {
    __VERIFIER_assert(a > 0);
  }  else {
    __VERIFIER_assert(a == 0);
  }

  return a;
}

