// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

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

void dll_update(DLL head, int idx, int data) {
  if(NULL == head) {
    return;
  } else {
    DLL p = head;
    while(NULL != p && idx > 0) {
      p = p->next;
      --idx;
    }
    if(NULL != p) {
      p->data = data;
    }
  }
}

int main(void) {

  const int data = 5;
  
  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  b->prev = a;

  b = NULL;
  int i = 1;
  while(i > -1) {
    dll_update(a, i, 7);
    --i;
  }

  free(a->next);
  free(a);

  return 0;
}
