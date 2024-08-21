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

SLL create_node(int data) {
  SLL temp = (SLL) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void _assert(int x) {
  if(!x) {
    create_node(-1);
  }
}

void sll_check_data(SLL head, int expected) {
  if(head) {
    SLL temp = head->next;
    while(head != temp) {
      _assert(expected == temp->data);
      temp = temp->next;
    }
    _assert(expected = head->data);
  }
}

void sll_update_all(SLL head, int new_value) {
  if(head) {
    SLL temp = head->next;
    while(head != temp) {
      temp->data = new_value;
      temp = temp->next;
    }
    head->data = new_value;
  }
}

void sll_circular_destroy(SLL head) {
  if(head) {
    SLL p = head->next;
    while(head != p) {
      SLL q = p->next;
      free(p);
      p = q;
    }
    free(head);
  }
}

int main(void) {

  const int data_1 = 5;
  const int data_2 = 7;

  SLL a = create_node(data_1);
  SLL b = create_node(data_1);
  a->next = b;
  b->next = a;
  
  b = NULL;
  sll_check_data(a, data_1);
  
  sll_update_all(a, data_2);
  sll_check_data(a, data_2);

  sll_circular_destroy(a);

  return 0;
}
