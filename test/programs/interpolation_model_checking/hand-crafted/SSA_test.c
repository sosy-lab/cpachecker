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
  unsigned int y;
  int r = __VERIFIER_nondet_int();
  if (r > 0) {
     x = 4;
	 y = 2*x-1;
  }
  else {
     x = 0;
	 y = 3*x;
	 x = 4*y;
  }
  while (r != 0) {
	int z = 4;
    x += 4;
	z += x;
    __VERIFIER_assert(x % 4 == 0);
    __VERIFIER_assert(z % 4 == 0);
    r = __VERIFIER_nondet_int();
  }
  __VERIFIER_assert(x % 2 == 0);
  return 0;
}
