#include <stdlib.h>

int main(void) {

  int counter = 0;

  while (counter < 5) {
    counter++;
    
    if (4 == counter) {
      break;
    }
  }
  
  return (0);
}
