#include<assert.h>

int main() {
  int sum = 0;
  int i;
  for (i=0; i<1000; i++) {
    sum++;
  }
  assert(sum != i);
}
