extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  unsigned int w = __VERIFIER_nondet_uint();
  unsigned int x = w;
  unsigned int y = __VERIFIER_nondet_uint();
  unsigned int z = y;
  while (__VERIFIER_nondet_uint()) {
    if (__VERIFIER_nondet_uint()) {
      ++w; ++x;
    } else {
      --y; --z;
    }
  }
  __VERIFIER_assert(w == x && y == z);
  return 0;
}
