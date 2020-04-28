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
  unsigned int y = w + 1;
  unsigned int z = x + 1;
  while (__VERIFIER_nondet_uint()) {
    y++;
    z++;
  }
  __VERIFIER_assert(y == z);
  return 0;
}
