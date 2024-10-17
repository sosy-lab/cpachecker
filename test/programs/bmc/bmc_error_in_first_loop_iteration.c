#include<assert.h>

void reach_error() { assert(0); }

extern int __VERIFIER_nondet_int(void);

int main() {
  int x = 0;
  while(__VERIFIER_nondet_int()) {
    if (x == 0) {
      // The error occurs only in the first unrolling of the loop
      goto ERROR;
    }
    x++;
  }
  return 0;
  ERROR: reach_error();
}
