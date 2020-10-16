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
  while(head) {
    SLL temp = head->next;
    _assert(expected == head->data);
    free(head);
    head = temp;
  }
}

void sll_prepend(SLL* head, int data) {
  SLL new_head = node_create(data);
  new_head->next = *head;
  *head = new_head;
}

int main(void) {

  const int data = 5;

  SLL a = node_create(data);
  SLL b = node_create(data);
  a->next = b;

  b = NULL;
  sll_prepend(&a, data);

  sll_check_and_destroy(a, data);

  return 0;
}
