#include <stdlib.h>

int main(void) {

  int counter;

  for (counter = 0; counter < 5; counter++) {
    
    if (2 == counter) {
      continue;
    }
    
    if (3 == counter) {
      continue;
    }
    
    if (4 == counter) {
      continue;
    }
  }

  return (0);
}
