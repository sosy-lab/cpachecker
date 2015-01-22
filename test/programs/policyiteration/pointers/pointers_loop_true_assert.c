#include<assert.h>

int main() {
  int sum = 0;
  int* p = &sum;
  for (int i=0; i<10; i++) {
    (*p)++;
  }
  assert(sum == 10);
}
