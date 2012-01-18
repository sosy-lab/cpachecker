#include <stdlib.h>

int main(void) {

  int counter = 0;

  while (counter < 5) {
    counter++;
  
    int counter2 = 10;
    while(counter2 > 0) {
      counter2--;
    }
    
    if (10 == counter) {
      goto ERROR;
    }
  }

  while (counter < 10) {
  counter++;
  }
  
  return (0);

  ERROR:
    goto ERROR;

}
