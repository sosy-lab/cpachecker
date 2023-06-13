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

void dll_destroy(DLL head) {
  if(head) {
    DLL p = head->next;
    while(p != head) {
      DLL q = p->next;
      free(p);
      p = q;
    }
    free(head);
  }
}

int _assert(int x) {
  if(!x) {
    // create memory leak
    node_create(-1);
    return 1;
  }
  return 0;
}

int dll_check_data(DLL head, int expected) {
  if(head) {
    while(head != head->next) {
      DLL temp = head->next;
      int ret = _assert(expected == head->data);
      if(ret) {
	return ret;
      }
      head = temp;
    }
  }
  return 0;
}

void dll_circular_prepend(DLL* head, int data) {
  DLL new_head = node_create(data);
  if(NULL == *head) {
    *head = new_head;
    new_head->next = new_head;
    new_head->prev = new_head;
  } else {
    DLL last = (*head)->prev;
    DLL old_head = *head;
    *head = new_head;
    new_head->next = old_head;
    old_head->prev = new_head;
    last->next = new_head;
    new_head->prev = last;
  }
}

int main(void) {

  const int data_1 = 5;
  const int data_2 = 7;
  
  DLL a = node_create(data_1);
  DLL b = node_create(data_1);
  a->next = b;
  b->prev = a;

  a->prev = b;
  b->next = a;
  
  b = NULL;
  dll_circular_prepend(&a, data_1);

  // expected to fail!
  int ret = dll_check_data(a, data_2);
  if(ret) {
    return ret;
  }
  
  dll_destroy(a);
  
  return 0;
}
