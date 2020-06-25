extern void __VERIFIER_error() __attribute__ ((__noreturn__));

/*
 * Simple example: build a list with only 1s and finally a 0 (arbitrary length); 
 * afterwards, go through it and check if the list does have the correct form, and in particular
 * finishes by a 0.
 */
#include <stdlib.h>

void exit(int s) {
	_EXIT: goto _EXIT;
}

typedef struct node {
  int h;
  struct node *n;
} *List;

int main() {
  /* Build a list of the form 0->1->...->29->30 */
  List a = (List) malloc(sizeof(struct node));

  if (a == 0) exit(1);

  List t;
  List p = a;
  
  int i = 0;
  
  while (i < 4 && __VERIFIER_nondet_int()) {  
    p->h = i;
    t = (List) malloc(sizeof(struct node));

    if (t == 0) exit(1);

    p->n = t;
    p = p->n;
    i++;
  }
  
  p->h = i;
  p->n = 0;
  p = a;
  i = 0;
  while (p!=0) {
    if (p->h != 1) {
      ERROR: __VERIFIER_error();
    }
    p = p->n;
    i++;
  }
  return 0;
}
