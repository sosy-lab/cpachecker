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
  struct node *next;
  int data;
} *SLL;

void main() {

  SLL list = malloc(sizeof(struct node));
  SLL beginning = list;

  while (__VERIFIER_nondet_int()) {
    list->data = 1;
    list->next = malloc(sizeof(struct node));
    list = list->next;
  }

  list->data = 3;
  list = beginning;

  while(list->data == 1) {
    list = list->next;
  }

   __VERIFIER_assert(list->data == 3);
}