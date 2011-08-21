#include <assert.h>
int choosing1 = 0, choosing2 = 0; // N boolean flags
int number1 = 0, number2 = 0; // N natural numbers
int x; //variable to test mutual exclusion

void main() {
  int tmp;
  choosing1 = 1;
  tmp = number2 + 1;
  number1 = tmp;
  choosing1 = 0;
  while (choosing2 >= 1) {};
  while (number1 >= number2 && number2 > 0) {
    // condition to exit the loop is (number1<number2 \/ number2=0)
  }
  // begin: critical section
  x = 0;
  assert(x <= 0);
  // end: critical section
  number1 = 0;
}

