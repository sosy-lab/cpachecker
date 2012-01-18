#include <stdlib.h>

int main(void) {
   
  int counter = 0;

LOOPSTART: 
  if (counter < 5) {
    counter++;
    goto LOOPSTART;
    
  } else if (counter < 10) {
    counter++;
    goto LOOPSTART;
    
  } else if (counter < 15) {
    counter++;
    goto LOOPSTART;
  }

if (counter == 4) {
  goto ERROR;
} else {
  goto END;
}

ERROR: 
  goto ERROR;


END:
  return (0);
}
