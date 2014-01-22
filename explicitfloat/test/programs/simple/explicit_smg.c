extern int __VERIFIER_nondet_int();
/*
 * Variation on example 0: use a (non-deterministic) boolean
 * flag to set the value of the elements in the list before
 * 3 to a value depending on the flag, and check later on
 * that the list is what has been built just before.
 */
#include <stdlib.h>
/*  #include "assert.h" */

void exit(int s) {
 _EXIT: goto _EXIT;
}

typedef struct node {
  int h;
  int flag;
  struct node *n;
} *List;

int main() {
  List p, a, t;

  /* Build a list of the form 1->2->2->...->1->3
   * with 1,2 depending on some flag
   */
  a = (List) malloc(sizeof(struct node));
  if (a == 0) exit(1);
  p = a;
  
  while (__VERIFIER_nondet_int()) {
  
    p->h = 1;

    t = (List) malloc(sizeof(struct node));
    
    if (t == 0) exit(1);

    p->n = t;
    p = p->n;
  }
  p->h = 3;
  p->n = 0;

  // Check it
  p = a;

  // as long as p->h is not equal to 3 ...
  while (p->h != 3) {
    // ... it has to be equal to 1, otherwise we got an error
    if (p->h != 1) {
      goto ERROR;
    }
    p = p->n;
  }

  // for he last element p->h then has to be equals to 3, otherwise we got an error
  if (p->h != 3)
    goto ERROR;

  // free memory
  p = a;
  while (p->n != 0) {
    t = p->n;
    free(p);
    p = t;
  }
  free(p);

  return 0;

  ERROR: goto ERROR;
}
