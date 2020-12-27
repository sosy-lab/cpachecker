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
  int data;
} *SLL;

SLL node_create(int data) {
  SLL temp = (SLL) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void _assert(int x) {
  if(!x) {
    node_create(-1);
  }
}

void sll_check_and_destroy(SLL head, int expected) {
  if(head) {
    SLL p = head->next;
    while(p != head) {
      SLL q = p->next;
      _assert(expected == p->data);
      free(p);
      p = q;
    }
    _assert(expected == head->data);
    free(head);
  }
}

int main(void) {

  const int data = 5;

  SLL a = node_create(data);
  SLL b = node_create(data);
  a->next = b;
  b->next = a;

  b = NULL;
  sll_check_and_destroy(a, data);

  return 0;
}
