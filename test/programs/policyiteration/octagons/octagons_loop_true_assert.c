#include<assert.h>

int main() {
  int sum = 0;

  for (int i=0; i<1000; i++) {
    sum++;
  }
  assert(sum == 1000);
}