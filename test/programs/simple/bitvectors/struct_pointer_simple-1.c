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
  if (*(tmp->ptr) != 3) {
    goto ERROR2;
  }
  printf ("SAFE\n");
  return 0;

ERROR2:
  printf ("UNSAFE\n");
ERROR:
  goto ERROR;

  return 1;
}
