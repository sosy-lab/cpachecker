#include <stdlib.h>
#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();
extern void*  __VERIFIER_nondet_ptr();

typedef struct node {
  int flag;
  struct node* next;
} node;

int main() {
  node *list = malloc(sizeof(node));
  list->flag = 2;
  list->next = __VERIFIER_nondet_ptr();

  while (__VERIFIER_nondet_bool()) {
    node* ptr = list;
    list = malloc(sizeof(node));
    list->next = ptr;
    list->flag = 1;
  }

  while(list->flag != 2) {
    node *ptr = list;
    list = list->next;
    free(ptr);
  }
  free(list);

  return 0;
}
