#include <assert.h>

int t1=0, t2=0; // N natural-number tickets
int x; // variable to test mutual exclusion

void main() {
  while (1) {
    t1 = t2 + 1;
    while( t1 >= t2 && ( t2 > 0 ) ) {}; // condition to exit the loop is (t1<t2 \/ t2=0)
    x=0;
    assert(x <= 0);
    t1 = 0;
  }
}

