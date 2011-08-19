#include <assert.h>

int cs1=0;
int cs2=0;
int lock=0;


void main() {
  while(1) {
    while(lock != 0);
    lock = 1;
    cs2 = 1;
    assert(cs1 == 0);
    cs2 = 0;
    lock = 0;
  }
}


