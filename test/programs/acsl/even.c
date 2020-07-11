extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern int __VERIFIER_nondet_int();

int main(void) {
  unsigned int x = 0;
  /*@ loop invariant  x % 2 == 0;*/
  while (__VERIFIER_nondet_int()){
    x += 2;
    __VERIFIER_assert(!(x%2));
  }
  return 0;
}
