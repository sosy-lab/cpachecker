#include <math.h>
#include <stdlib.h>

void reach_error() {
  abort();
}

int main() {
  if (isnan(sqrt(-1))) {
    reach_error();
  }
}
