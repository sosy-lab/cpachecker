#include <assert.h>

int g=0;
int r=0;

void main() {
  r--;
  g++;
  assert(r != 1);
}
