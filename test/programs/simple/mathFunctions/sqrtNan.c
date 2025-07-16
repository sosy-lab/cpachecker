#include <math.h>
#include <stdlib.h>

void reach_error() {
  abort();
}

int main() {
  if (isnan(sqrt(NAN))) {
    reach_error();
  }
}
