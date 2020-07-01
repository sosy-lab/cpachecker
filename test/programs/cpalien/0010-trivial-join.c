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

  if (__VERIFIER_nondet_bool()) {
    nd->next = nd;
    nd->prev = nd;
  } else {
    nd->next = NULL;
    nd->prev = NULL;
  }

  free(nd);
  return 0;
}
