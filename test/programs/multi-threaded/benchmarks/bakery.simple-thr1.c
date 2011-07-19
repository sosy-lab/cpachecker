#include <assert.h>

int t1=0, t2=0; // N natural-number tickets
int x; // variable to test mutual exclusion

void main() {
  while (1) {
    t2 = t1 + 1;
    while( t2 >= t1 && ( t1 > 0 ) ) {}; // condition to exit the loop is (t2<t1 \/ t1=0)
    x = 1;
    assert(x >= 1);
    t2 = 0;
  }
}
