extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern int __VERIFIER_nondet_int();
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  unsigned int x;
  int r = __VERIFIER_nondet_int();
  if (r > 0) {
     x = 4;
  }
  else {
     x = 0;
  }
  while (r != 0) {
    x += 4;
    __VERIFIER_assert(x % 4 == 0);
    r = __VERIFIER_nondet_int();
  }
  __VERIFIER_assert(x % 2 == 0);
  return 0;
}
