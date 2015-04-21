#include<assert.h>

int main() {
  for (int i=0; i<100; i++) {
    for (int k=0; k<1000; k++) {
      assert(k==0);
    }
  }
}
