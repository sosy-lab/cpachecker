void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

extern float __VERIFIER_nondet_float();

int main(void) {
  unsigned int a = __VERIFIER_nondet_float(); //interval from -infinity to infinity

  if (a > 0) {
    return a;
  }

  __VERIFIER_assert(a <= 0);

  return a;
}

