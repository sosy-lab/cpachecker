// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();
extern void*  __VERIFIER_nondet_ptr();

typedef struct node {
  int flag;
  struct node* next;
} node;

int main() {
  node *list = malloc(sizeof(node));
  list->flag = 2;
  list->next = __VERIFIER_nondet_ptr();

  while (__VERIFIER_nondet_bool()) {
    node* ptr = list;
    list = malloc(sizeof(node));
    list->next = ptr;
    list->flag = 1;
  }

  while(list->flag != 3) {
    node *ptr = list;
    list = list->next;
    free(ptr);
  }
  free(list);

  return 0;
}
