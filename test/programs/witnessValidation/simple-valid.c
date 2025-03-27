#include <stdlib.h>

int main() {

  int* x = (int*) malloc(6*sizeof(int));
  //@assert \valid(x)

  for (int i = 0; i < 6; i++) {
    x[i] = i;
  }
  //@assert \valid(x)

  free(x);
  //@assert \valid(x)

  free(x); //To export violation witnesses
  return 0;

}