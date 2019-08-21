#include <stdlib.h>

int main(void) {

  int counter;
  
  for (counter = 0; counter < 5; counter++) {
    
    LABELTEST:
    int a;
  }
  
  if (counter < 10) {
    goto LABELTEST;
  }

  return (0);
}
