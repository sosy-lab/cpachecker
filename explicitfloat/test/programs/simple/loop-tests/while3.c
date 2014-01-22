#include <stdlib.h>

int main(void) {

  int counter = 0;

  while (counter < 5) {
    counter++;
  
    int counter2 = 10;
    while(counter2 > 0) {
      counter2--;
    }
  }

  while (counter < 10) {
    counter++;
  }

  return (0);
}
