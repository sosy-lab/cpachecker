#include <assert.h>
// the program is unsafe: cs1=1 and cs2=1 can occur at the same time
int g = 0;
int cs1 = 0;
int cs2 = 0;

void main() {
  cs1 = 1;
  g = 1;
  assert(cs2 == 0);
  cs1 = 0;
}
