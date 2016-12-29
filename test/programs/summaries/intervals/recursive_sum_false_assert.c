#include<assert.h>

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int condition);

int sum(int i) {
  if (i <= 0) {
    return i;
  }
  return i + sum(i - 1);
}

int main() {
  int i = __VERIFIER_nondet_int();
  int sum = sum(i);
  assert(sum >= 0);
}
