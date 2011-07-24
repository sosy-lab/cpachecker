#include <assert.h>
// the program is safe
int g = 0;
int cs1 = 0;
int cs2 = 0;

void main() {
  while (g != 1);
  g = 0;
  cs2 = 1;
  assert(cs1 == 0);
}
