#include<assert.h>

extern int __VERIFIER_nondet_int();

int main() {
  int x = 5;
  int y = 10;
  int nondet = __VERIFIER_nondet_int();
  int p;
  int counter;
  if (nondet) {
    y = 100;
    p = 1;
  } else {
    p = 2;
  }

  int set;
  while (__VERIFIER_nondet_int()) {
    x++;
    counter = 100;
    while (__VERIFIER_nondet_int()) {
      y++;
    }
    assert(counter == 100);
    while (__VERIFIER_nondet_int()) {
      counter++;
    }
    for (int z = 0; z < 100; z++) {}
    if (p == 1 || p == 2) {
      set = 1;
    }
    for (int z = 0; z < 100; z++) {}
    assert(set == 1);
  }

  assert((nondet && p == 1) || (!nondet && p == 2));
}
