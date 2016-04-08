#include<assert.h>

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
  }

  assert(counter == 100);
}
