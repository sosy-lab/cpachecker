#include <stdlib.h>
#include <stdio.h>

struct node {
  void* ptr;
};


int main() {
  struct node f;
  struct node g;
  
  f.ptr = &g;
  ((struct node*)f.ptr)->ptr = 0;
  
  if (g.ptr != 0) {
    goto ERROR;
  }
  printf ("SAFE\n");
  return 0;

ERROR:
  printf ("UNSAFE\n");
ERROR2:
  goto ERROR2;

  return 1;
}
