#include <math.h>
#include <stdlib.h>

void reach_error() {
  abort();
}

int main() {
  if (sqrt(2) == 1.4142135623730951) {
    reach_error();
  }
}
