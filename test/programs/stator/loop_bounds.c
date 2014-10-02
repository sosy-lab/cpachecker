#include<assert.h>

int main() {
  int i;
  for (i = 0; i<=100000; i++) {
  }
  assert(i == 100001);
  return 0;
}
