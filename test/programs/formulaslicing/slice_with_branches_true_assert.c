#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
  int x = 5;
  int y = 10;
  int nondet = __VERIFIER_nondet_int();
  int p;
  if (nondet) {
    y = 100;
    p = 1;
  } else {
    p = 2;
  }

  while (__VERIFIER_nondet_int()) {
    x++;
    y++;
  }

  assert((nondet && p == 1) || (!nondet && p == 2));
}