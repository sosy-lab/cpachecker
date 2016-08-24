#include<assert.h>

int main() {
  int a = 10;
  int b = 100;
  int *p;
  p = &a;
  for (int i=0; i<10; i++) {
    p = &b;
  }
  *p = 20;
  assert(a == 20);

}