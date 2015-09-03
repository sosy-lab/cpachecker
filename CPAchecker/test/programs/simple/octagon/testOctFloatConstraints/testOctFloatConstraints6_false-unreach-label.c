void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

int main(void) {
  float a = 1.5;
  float b = 2.5;

  __VERIFIER_assert(a > b); 

  return 0;
}

