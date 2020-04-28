extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  int x = 4;
  while (1) {
    __VERIFIER_assert(x > 0);
    x = (x * 2) - 2;
  }
  return 0;
}
