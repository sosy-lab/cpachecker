void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

int main(void) {
  int a = 3;
  int b = 2;

  __VERIFIER_assert(a == b); 

  return 0;
}

