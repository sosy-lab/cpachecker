void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: goto ERROR;
  }
  return;
}

int main(void) {
  int a = __VERIFIER_nondet_uint(); //interval from 0 to infinity

  if (a < 10) {
    __VERIFIER_assert(0);
  } else {
//     __VERIFIER_assert(0);
  } 

  return a;
}

