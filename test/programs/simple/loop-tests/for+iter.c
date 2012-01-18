#include <stdlib.h>

int main(void) {

  int counter;

  for (counter = 0; counter < 5; counter) {
    int a;
    counter++;
  }
  
  for (counter = 0; counter < 5; ) {
    int b;
    counter++;
  }

  return (0);
}
