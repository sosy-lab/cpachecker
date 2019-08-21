extern void __VERIFIER_error() __attribute__ ((__noreturn__));

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

void main() {  
  List p, a, t;

  /* Build a list of the form 1->2->2->...->1->3
   * with 1,2 depending on some flag
   */
  a = (List) malloc(sizeof(struct node));
  if (a == 0) exit(1);
  p = a;
  
  int i = 0;
  while (i < 4 && __VERIFIER_nondet_int()) {
    i++;
    
    p->flag = __VERIFIER_nondet_int();
  
    if (p->flag) {
      p->h = 1;
    } else {
      p->h = 2;
    }

    t = (List) malloc(sizeof(struct node));

    if (t == 0) exit(1);

    p->n = t;
    p = p->n;
  }
  p->h = 3;
    
  /* Check it */
  p = a;
 
  while (p->h != 3) {
    if (p->flag) {
        if (p->h != 1)
            goto ERROR;
    } else {
        if (p->h != 2)
            goto ERROR;
    }    
    p = p->n;
    i++;
  }
            
  if (p->h != 3 || i > 20)
    goto ERROR;

  return;

  ERROR: __VERIFIER_error();
}
