extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  int r = __VERIFIER_nondet_int();
  unsigned int x = 0;
  if (r > 0) {
     x += 2;
  }
  else {
     x += 4;
  }
  while (r != 0) {
    x += 2;
    r = __VERIFIER_nondet_int();
  }
  __VERIFIER_assert(!(x % 2));
  return 0;
}
