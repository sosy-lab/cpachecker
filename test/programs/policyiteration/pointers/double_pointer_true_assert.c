#include<assert.h>

int main() {
  int a = 1;
  int *b = &a;
  int **c = &b;
  **c = 42;
  assert(a == 42);
}
