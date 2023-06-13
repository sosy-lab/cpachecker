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
  while(head != NULL) {
    _assert(expected == head->data);
    head = head->next;
  }
}

void dll_destroy(DLL head) {
  while(head != NULL) {
    DLL temp = head->next;
    free(head);
    head = temp;
  }
}

void append_to_dll(DLL* head, int data) {
  DLL new_last = node_create(data);
  if(NULL == *head) {
    *head = new_last;
  } else {
    DLL last = *head;
    while(NULL != last->next) {
      last = last->next;
    }
    last->next = new_last;
    new_last->prev = last;
  }
}

int main(void) {

  const int data_1 = 5;
  const int data_2 = 7;

  DLL a = node_create(data_1);
  DLL b = node_create(data_1);
  a->next = b;
  b->prev = a;

  b = NULL;
  append_to_dll(&a, data_2);

  // next line should fail!
  dll_check_data(a, data_1);

  dll_destroy(a);
  
  return 0;
}
