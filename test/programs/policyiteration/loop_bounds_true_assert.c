#include<assert.h>

int main() {
  int i;
  int sum = 0;
  for (i = 0; i<100000; i++) {
    sum++;
  }
  assert(i == 100000);
  return 0;
}
