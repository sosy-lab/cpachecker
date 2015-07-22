#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
  int x = 0;
  int p;

  int z = __VERIFIER_nondet_int();
  if (z > 100) {
    p = 0;
  } else {
    p = 1;
  }

  while (__VERIFIER_nondet_int()) {
    x++;
  }

  // todo: for some reason CPAchecker gets confused in the absence of brackets.
  assert((z > 100 && p == 0)
          || (z <= 100 && p == 1));
}
