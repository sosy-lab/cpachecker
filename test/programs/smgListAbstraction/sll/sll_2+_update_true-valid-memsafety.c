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

void sll_check_data(SLL head, int expected) {
  while(head) {
    SLL temp = head->next;
    _assert(expected == head->data);
    head = temp;
  }
}

void sll_update_all(SLL head, int new_value) {
  while(head) {
    SLL temp = head->next;
    head->data = new_value;
    head = temp;
  }
}

void sll_destroy(SLL head) {
  while(head) {
    SLL temp = head->next;
    free(head);
    head = temp;
  }
}

int main(void) {

  const int data_1 = 5;
  const int data_2 = 7;

  SLL a = node_create(data_1);
  SLL b = node_create(data_1);
  a->next = b;

  b = NULL;
  sll_check_data(a, data_1);

  sll_update_all(a, data_2);
  sll_check_data(a, data_2);

  sll_destroy(a);

  return 0;
}
