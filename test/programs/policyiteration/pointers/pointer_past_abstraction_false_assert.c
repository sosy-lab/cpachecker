#include<assert.h>

int main() {
  int a = 0;
  int *p = &a;
  int i;
  for (i=0; i<10; i++) {
  }
  assert(i <= 10);
  *p = 10;
  assert(a == 0);
}