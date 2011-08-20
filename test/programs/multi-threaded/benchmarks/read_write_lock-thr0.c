#include <assert.h>
int w=0, r=0, x, y;


void main() { //writer
  assert(w==0);
  assert(r==0);
  w = 1;
  x = 3;
  w = 0;
}


