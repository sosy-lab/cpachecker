#include <assert.h>
// the program is safe
int g = 0;
int cs1 = 0;
int cs2 = 0;
int pc1 = 0;

void main() {
  cs1 = 1;
  assert(cs2 == 0);
  cs1 = 0;
  pc1 = 1;
  g = 1;
}
