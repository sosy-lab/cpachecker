#include <stdlib.h>

int main(void) {

  int counter = 0;

  for (;counter < 5; counter++) {
    int a;
    a++;
  }
  
  for (counter = 15; counter < 5; counter) {
    int b;
    b++;
  }

  for (counter = 15; counter < 5; ) {
    int c;
    c++;
  }
  
  return (0);
}
