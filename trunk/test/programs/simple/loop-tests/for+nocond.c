#include <stdlib.h>

int main(void) {

  int counter = 0;
  
  for (; ; counter++) {
    int a;

    if (counter > 5) {
      break;
    }
  }

  return (0);
}
