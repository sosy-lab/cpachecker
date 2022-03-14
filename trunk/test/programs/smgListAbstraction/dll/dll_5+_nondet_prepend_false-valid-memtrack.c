// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

typedef struct node {
  struct node* next;
  struct node* prev;
  int data;
} *DLL;

DLL node_create(int data) {
  DLL temp = (DLL) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

void _assert(int x) {
  if(!x) {
    // invalid memory leak
    node_create(-1);
  }
}

int main() {

  const int data = 5;

  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;

  b = NULL;
  while(__VERIFIER_nondet_int()) {
    DLL temp = node_create(data);
    temp->next = a;
    a = temp;
  }

  while(NULL != a) {
    DLL temp = a->next;
    _assert(data == a->data);
    free(a);
    a = temp;
  }

  return 0;
}
