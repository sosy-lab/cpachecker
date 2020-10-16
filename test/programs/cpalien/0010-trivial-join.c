// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <stdbool.h>

struct node;
extern bool __VERIFIER_nondet_bool();

typedef struct node {
  struct node *next;
  struct node *prev;
} node;

int main() {
  node* nd = malloc(sizeof(node));

  if (__VERIFIER_nondet_bool()) {
    nd->next = nd;
    nd->prev = nd;
  } else {
    nd->next = NULL;
    nd->prev = NULL;
  }

  free(nd);
  return 0;
}
