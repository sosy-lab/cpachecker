#include<assert.h>

int main() {
  int sum = 0;
  /*int *p = &sum;*/
  /*if (sum == 0) {*/
    /*sum += 1;*/
  /*}*/
  int* p = &sum;
  for (int i=0; i<10; i++) {
    (*p)++;
  }
  assert(sum == 10);
}
