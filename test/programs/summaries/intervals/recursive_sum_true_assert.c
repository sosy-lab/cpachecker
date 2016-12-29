#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);

int sum(int i) {
  if (i == 0) {
    return 0;
  }
  return i + sum(i - 1);
}

int main() {
  int i = __VERIFIER_nondet_int();
  __VERIFIER_assume(i > 0);
  int out = sum(i);
  assert(out >= 0);
}