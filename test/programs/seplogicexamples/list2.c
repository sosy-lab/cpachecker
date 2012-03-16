/*
 * Simple example: build a list with only 1s, then 2s and finally
 * on 3 (arbitrary length); afterwards, go through it and check
 * if the the list does have the correct form, and in particular
 * finishes by a 3.
 */
#include <stdlib.h>

typedef struct node {
  struct node *n;
  int h;
} *List;

int main() {
  /* Build a list of the form 1->...->1->2->....->2->3 */
  List a = (List) malloc(sizeof(struct node));
  if (a == 0) goto ERROR;
  List t;
  List p = a;
  p->n = 0;
  while (__VERIFIER_nondet_int()) {
    p->h = 1;
    t = (List) malloc(sizeof(struct node));
    if (t == 0) goto ERROR;
    t->n = 0;
    p->n = t;
    p = p->n;
  }
  p->h = 3;

    
  /* Check it */
  p = a;
  while (p->h == 1)
    p = p->n;
  if(p->h != 3)
    ERROR: goto ERROR;

  return 0;
}
