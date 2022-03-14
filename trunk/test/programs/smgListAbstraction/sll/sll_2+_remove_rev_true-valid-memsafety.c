// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

typedef struct node {
  struct node *next;
  int data;
} *SLL;

SLL node_create(int data) {
  SLL temp = (SLL) malloc(sizeof(struct ode));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void sll_reverse_destroy(SLL head) {
  while(head) {
    SLL second_to_last = NULL;
    SLL last = head;
    while(last->next) {
      second_to_last = last;
      last = last->next;
    }
    free(last);
    if(second_to_last) {
      second_to_last->next = NULL;
    }
  }
}

int main(void) {

  const int data = 5;

  SLL a = node_create(data);
  SLL b = node_create(data);
  a->next = b;

  b = NULL;
  sll_reverse_destroy(a);

  return 0;
}
