#include <stdlib.h>

int main(void) {

  int counter = 0;
  
  for (; ; ) {
    int a;
    counter++;
    
    if (3 == counter){
      continue;
    }
    
    if (counter > 5){
      break;
    }
  }

  return (0);
}
