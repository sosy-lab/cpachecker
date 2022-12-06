// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

void reach_error() { assert(0); }
extern int __VERIFIER_nondet_int(void);
extern void abort(void);

void __VERIFIER_assert(int cond) {
    if (!(cond)) {
          ERROR: {reach_error();abort();}
                   }
      return;
}

typedef struct node {
  int data;
  struct node *next;
  struct node *prev;
} *List;

void main() {
  List curr = (List) malloc(sizeof(struct node));
  if (curr == 0) exit(1);
  List old = curr;
  curr->data = -3;
  curr->prev = 0;
  curr->next = 0;
  int length;
    
  for (int i = 0; i < __VERIFIER_nondet_int(); i++) {
    curr->next = (List) malloc(sizeof(struct node));
    if (curr->next == 0) exit(1);
    curr->next->prev = curr;
    curr = curr->next;
    curr->data = i;
    curr->next = 0;
    length = i + 1;
  }
  
  curr->data = 3;
  curr = old;

  while (curr->next != 0) {
    curr = curr->next;
  }
  assert(curr->data == 3);
  if (curr->prev != 0) {
    while (curr->prev != 0) {
      curr = curr->prev;
      length--;
    }
    assert(curr->data == -3);
    assert(length == 0);
  }
}