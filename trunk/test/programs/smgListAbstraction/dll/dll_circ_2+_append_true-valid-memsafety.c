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
  struct node *prev;
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
  if(NULL != head) {
    DLL temp = head->next;
    while(temp != head) {
      _assert(expected == temp->data);
      temp = temp->next;
    }
    _assert(expected == head->data);
  }
}

void dll_circular_destroy(DLL head) {
  if(NULL != head) {
    DLL p = head->next;
    while(p != head) {
      DLL q = p->next;
      free(p);
      p = q;
    }
    free(head);
  }
}

void dll_circular_append(DLL* head, int data) {
  DLL new_last = node_create(data);
  if(NULL == *head) {
    new_last->prev = new_last;
    new_last->next = new_last;
    *head = new_last;
  } else {
    DLL last = (*head)->prev;
    last->next = new_last;
    new_last->prev = last;
    new_last->next = *head;
    (*head)->prev = new_last;
  }
}

int main(void) {

  const int data = 5;

  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  b->prev = a;

  a->prev = b;
  b->next = a;

  b = NULL;
  dll_circular_append(&a, data);
  dll_check_data(a, data);
  dll_circular_destroy(a);
  
  return 0;
}
