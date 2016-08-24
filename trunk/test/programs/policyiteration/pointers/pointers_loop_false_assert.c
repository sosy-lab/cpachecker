#include<assert.h>

int main() {
  int sum = 0;
  int *p = &sum;
  for (int i=0; i<1000; i++) {
    *p = *p + 1;
  }
  assert(sum == 0);
}
