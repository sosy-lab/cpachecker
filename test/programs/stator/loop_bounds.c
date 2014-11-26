#include<assert.h>

int main() {
  int i;
  for (i = 0; i<=100000; i++) {
  }

  // TODO : with integers support the exact equality.
  assert(i <= 100001);
  return 0;
}
