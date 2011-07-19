#include <assert.h>

int g=0;
int r=0;


void main() {
  while(g==0);
  r++;
  assert(r != 1);
}
