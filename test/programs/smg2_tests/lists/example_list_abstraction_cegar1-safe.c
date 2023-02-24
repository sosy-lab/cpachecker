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

void main() {
  List curr = (List) malloc(sizeof(struct node));
  if (curr == 0) exit(1);
  // Create a list element with -3 as value
  List old = curr;
  curr->next = 0;
  curr->prev = 0;
  curr->data = -3;
  // We track the length of the list
  int length = 0;

  // Create nondet # of elements in the list, with the value being 
  for (int i = 0; i < __VERIFIER_nondet_int(); i++) {
    curr->next = (List) malloc(sizeof(struct node));
    if (curr->next == 0) exit(1);
    curr->next->prev = curr;
    curr = curr->next;
    curr->next = 0;
    curr->data = 1;
    length++;
  }
  
  while (curr->prev != 0) {
    length--;
    curr = curr->prev;
  }
  // Assert that the first element is -3 in value (all others are 1)
  // If we track length, we never end up here, as the changing states 
  // are not covered by each other due to the different lengths. This needs CEGAR!
  assert(curr->data == -3);
}