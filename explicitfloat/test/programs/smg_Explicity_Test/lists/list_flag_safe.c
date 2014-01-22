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
  while (i < 1 && __VERIFIER_nondet_int()) {
    i++;
  
    p->flag = __VERIFIER_nondet_int();
  
    if (p->flag) {
	 p->flag = 1;
      p->h = 1;
__VERIFIER_BUILTIN_PLOT("A1");
    } else {
      p->flag = 0;
      p->h = 2;
__VERIFIER_BUILTIN_PLOT("A2");
    }

    t = (List) malloc(sizeof(struct node));
    
    if (t == 0) exit(1);

    p->n = t;
    p = p->n;
  }
  p->h = 3;
    
  /* Check it */
  p = a;
 
  i = 0;

if(p->flag) {
__VERIFIER_BUILTIN_PLOT("D1");
} else {
__VERIFIER_BUILTIN_PLOT("D2");
}

  while (p->h != 3) {
    if (p->flag) {
	__VERIFIER_BUILTIN_PLOT("C2");
        if (p->h != 1)
            goto ERROR;
    } else {
	__VERIFIER_BUILTIN_PLOT("C3");
        if (p->h != 2)
            goto ERROR;
    }    
    p = p->n;
    i++;
  }

__VERIFIER_BUILTIN_PLOT("C4");
            
  if (p->h != 3)
    goto ERROR;

  return;

  ERROR: goto ERROR;
}
