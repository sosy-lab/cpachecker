#include <stdlib.h>
#include <stdbool.h>

struct node;
extern bool __VERIFIER_nondet_bool();

typedef struct node {
  struct node *next;
  struct node *prev;
} node;

int main() {
  node* nd = malloc(sizeof(node));
  nd->next = NULL;

  while (__VERIFIER_nondet_bool()) {
    if (nd->next == NULL) {
      nd->next = nd;
    }
    else {
      nd->next = NULL;
    }
  }

  free(nd->next);
  nd = (void*)0;
  return 0;
}
