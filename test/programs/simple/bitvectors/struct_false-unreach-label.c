#include <stdlib.h>
#include <stdio.h>

struct node {
    struct node     *next;
    int             value;
};


int main() {
  struct node f;
  f.value = 0;
  f.next = 0;
  struct node* tmp = malloc(sizeof(struct node));
  tmp->value = 1;
  tmp->next = 0;
  f = *tmp;
  if (f.value == 1) {
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
