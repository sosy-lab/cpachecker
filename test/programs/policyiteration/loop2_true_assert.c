#include<assert.h>

int main() {
  int sum = 0;
  int i;
  for (i=0; i<=1000; i++) {
    sum += 1;
  }

  // TODO: with integers we should be able to assert equality.
  assert(i <= 1001);
}
