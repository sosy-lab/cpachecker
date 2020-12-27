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

void _assert(int x) {
  if(!x) {
    // create memory leak
    node_create(-1);
  }
}

void dll_check_data(DLL head, int expected) {
  while(NULL != head) {
    DLL temp = head->next;
    _assert(expected == head->data);
    head = temp;
  }
}

void dll_prepend(DLL* head, int data) {
  DLL old_head = *head;
  *head = node_create(data);
  (*head)->next = old_head;
  old_head->prev = *head;
}

void dll_destroy(DLL head) {
  while(NULL != head) {
    DLL temp = head->next;
    free(head);
    head = temp;
  }
}

int main(void) {

  const int data = 5;

  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  b->prev = a;

  // remove external pointer
  b = NULL;
  dll_prepend(&a, data);
  dll_check_data(a, data);
  dll_destroy(a);

  return 0;
}
