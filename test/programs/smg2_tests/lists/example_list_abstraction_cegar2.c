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
  List old = curr;
  curr->next = 0;
  curr->prev = 0;
  curr->data = -3;
    
  for (int i = 0; i < __VERIFIER_nondet_int(); i++) {
    curr->next = (List) malloc(sizeof(struct node));
    if (curr->next == 0) exit(1);
    curr->next->prev = curr;
    curr = curr->next;
    curr->next = 0;
    curr->data = i;
  }
  
  // curr = old;
  // int length = 0;
  while (curr->prev != 0) {
    //length++;
    curr = curr->prev;
  }
  assert(curr->data == -3);
}