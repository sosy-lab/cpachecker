#include <assert.h>
int w, r, x, y;

void main() { //reader
  assert(w==0);
  r = r+1;
  y = x;
  assert(y == x);
  r = r-1;
}
