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
  c = *pointer;

  // Convex hull of two points.
  assert(c >= 1 && c <= 10);
}
