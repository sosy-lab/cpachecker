#include <stdlib.h>
#include <stdio.h>
//typedef unsigned int size_t;
//extern  __attribute__((__nothrow__)) void *malloc(size_t __size )  __attribute__((__malloc__)) ;

struct node {
    short int      next;
    short int      value;
};


int main() {
  struct node f;
  struct node* tmp;
  tmp = malloc(sizeof(struct node));
  f = *tmp;
  tmp->value = 1;
  tmp->next = 0;
  if (f.value != 1) {
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
