#include <stdlib.h>
#include <assert.h>
int main() {
  int size = 100;
  int num = __VERIFIER_nondet_int();
  int * arr = malloc(sizeof(int) * size);
  for (int i = 0; i < size; i++) {
    arr[i] = num;
    num++;
  }
  for (int i = size; i >= 0; i--) {
    assert(*(arr + i) == num);
    num--;
  }
  return 0;
}
