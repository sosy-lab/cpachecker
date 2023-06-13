// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

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

void sll_append(SLL* head, int data) {
  SLL new_node = node_create(data);
  if(NULL == *head) {
    *head = new_node;
  } else {
    SLL temp = *head;
    while(temp->next) {
      temp = temp->next;
    }
    temp->next = new_node;
  }
}

void _assert(int x) {
  if(!x) {
    node_create(-1);
  }
}

int sll_length(SLL head) {
  int len = 0;
  while(head) {
    len++;
    head = head->next;
  }
  return len;
}

void free_sll(SLL head) {
  while(head) {
    SLL temp = head->next;
    free(head);
    head = temp;
  }
}

int main(void) {

  const int data = 1;
  const int len = 5;

  SLL lst = NULL;

  int i = 0;
  while(i < len) {
    sll_append(&lst, data);
    i++;
  }

  _assert(len == sll_length(lst));

  free_sll(lst);

  return 0;
}
