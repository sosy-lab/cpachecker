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

typedef struct node {
  int data;
  struct node* next;
} node;

extern bool __VERIFIER_nondet_bool();
extern int __VERIFIER_nondet_int();

int main() {
  node* root = NULL;

  while (__VERIFIER_nondet_bool()){
    node* tmp = malloc(sizeof(node));
    tmp->data = __VERIFIER_nondet_int();
    tmp->next = root;
    root = tmp;
  }

  while(root != NULL){
    node *tmp = root;
    root = root->next;
    free(tmp);
  }
}
