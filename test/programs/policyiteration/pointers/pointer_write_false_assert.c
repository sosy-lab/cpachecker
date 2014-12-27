#include<assert.h>

int main() {
  int a, b, c, undefined;
  int *pointer;
  a = 1;
  b = 10;
  if (undefined) {
    pointer = &a;
  } else {
    pointer = &b;
  }
  *pointer = 42;

  // This assertion should fail, as *pointer may alias to <a>.
  assert(a != 42);
  return 0;
}
