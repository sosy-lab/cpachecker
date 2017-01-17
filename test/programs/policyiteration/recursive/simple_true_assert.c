#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
  int i=0;
  int a = __VERIFIER_nondet_int();

  if (a <= 0) {
    return 1;
  }

  while (__VERIFIER_nondet_int()) {
    i += a;
  }
  assert(i >= 0);
  return 0;
}