#include <stdlib.h>

int square(x){
  return (x*x);
}

int cube(x){
  return (x*x*x);
}

int main(void) {

  int counter;
  int x = 3;
  
  for (counter = square(x); counter < 5; counter = cube(counter)) {
    int a = 0;
  }

  for (x = 2; x < 5; x = cube(x)) {
    int a = 0;
  }

  return (0);
}
