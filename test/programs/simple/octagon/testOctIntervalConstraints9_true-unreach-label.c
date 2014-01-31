void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

extern int __VERIFIER_nondet_int();

int main(void) {
  unsigned int a = __VERIFIER_nondet_int(); //interval from -infinity to infinity

  if (a < 0) {
    return;
  }

  __VERIFIER_assert(a >= 0);

  return a;
}

