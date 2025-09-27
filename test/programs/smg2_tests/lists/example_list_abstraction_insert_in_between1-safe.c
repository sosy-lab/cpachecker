// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

typedef struct node {
  int data;
  struct node *next;
  struct node *prev;
} *List;

// Make a list, but with one concrete value at the end, 
// and inserts such that new elements are only inserted before that last elem
void main() {
  List curr = (List) malloc(sizeof(struct node));
  if (curr == 0) exit(1);
  // Create a list element with -3 as value as last elem
  List last = curr;
  curr->next = 0;
  curr->prev = 0;
  curr->data = -3;

  // Create nondet # of elements in the list, with the value being 1 (distinct from the last)
  for (int i = 0; i < __VERIFIER_nondet_int(); i++) {
    curr->prev = (List) malloc(sizeof(struct node));
    if (curr->prev == 0) exit(1);
    curr->prev->next = curr;
    curr = curr->prev;
    curr->prev = 0;
    curr->data = 1;
  }
  
  // Rewind to the beginning
  while (curr->prev != 0) {
    curr = curr->prev;
  }

  // Make the program memsafe
  while(curr->next != 0) {
    curr = curr->next;
    free(curr->prev);
  }
  // Assert that the last element is -3 in value (all others are 1)
  assert(curr->data == -3);
  free(curr);
}