#include <stdlib.h>
#include <stdio.h>
//typedef unsigned int size_t;
//extern  __attribute__((__nothrow__)) void *malloc(size_t __size )  __attribute__((__malloc__)) ;

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
  t = 5;
  if (*(tmp->ptr) != 5) {
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
