#include<assert.h>

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}
extern _Bool __VERIFIER_nondet_bool();
extern int __VERIFIER_nondet_int();

int main() {
  int i = __VERIFIER_nondet_int();
  int k = __VERIFIER_nondet_int();
  int c = i + 2 * k;
  while (__VERIFIER_nondet_bool()) {
    int diff = __VERIFIER_nondet_int();

    i += 2 * diff;
    k += diff;
    c += 4 * diff;
  }
  __VERIFIER_assert(i + 2 * k == c);
}