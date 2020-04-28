extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  unsigned int s = 0;
  while (__VERIFIER_nondet_uint()) {
    if (s != 0) {
      ++s;
    }
    if (__VERIFIER_nondet_uint()) {
      __VERIFIER_assert(s == 0);
    }
  }
  return 0;
}
