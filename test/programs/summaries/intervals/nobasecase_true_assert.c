#include<assert.h>

extern int __VERIFIER_nondet_int();

int f(int x);

int main() {
  int z = f(__VERIFIER_nondet_int());
  assert(0);
}

int f(int x) {
  return x + f(x - 1);
}