// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);
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
  while(head) {
    SLL temp = head->next;
    _assert(expected == head->data);
    free(head);
    head = temp;
  }
}

void sll_append(SLL* head, int data) {
  SLL new_last = node_create(data);
  if(NULL == *head) {
    *head = new_last;
  } else {
    SLL last = *head;
    while(last->next) {
      last = last->next;
    }
    last->next = new_last;
  }
}

int main(void) {

  const int data = 5;

  SLL a = node_create(data);
  SLL b = node_create(data);
  a->next = b;

  b = NULL;
  while(__VERIFIER_nondet_int()) {
    sll_append(&a, data);
  }

  sll_check_and_destroy(a, data);
  
  return 0;
}
