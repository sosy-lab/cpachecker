#include <stdlib.h>

int main() {
  {
    int* x = (int*) malloc(5*sizeof(int));
  }
  return 0;
}