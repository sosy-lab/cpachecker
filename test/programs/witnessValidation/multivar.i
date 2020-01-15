extern void __VERIFIER_error() __attribute__ ((__noreturn__));
extern unsigned int __VERIFIER_nondet_uint(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
int main(void) {
  unsigned int x = __VERIFIER_nondet_uint();
  unsigned int y = x;
  while (x < 1024) {
    x++;
    y++;
  }
  __VERIFIER_assert(x == y);
}
