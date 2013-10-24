#include <stdlib.h>
#include <stdio.h>

struct node {
  int* ptr;
};

int main() {
  struct node f;
  struct node* tmp;
  int t = 3;
  int* ptr = &t;
  f.ptr = ptr;
  tmp = &f;
  *(tmp->ptr) = 4;
  if (t != 4) {
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
