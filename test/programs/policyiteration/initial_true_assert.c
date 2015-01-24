#include<assert.h>
extern _Bool __VERIFIER_nondet_bool();

int main() {
  int i=0;
  while (__VERIFIER_nondet_bool()) {
    for (i=0; i<10; i++) {
    }
    i += 10;
  }
  assert(i >= 0 && i <= 20);
}
