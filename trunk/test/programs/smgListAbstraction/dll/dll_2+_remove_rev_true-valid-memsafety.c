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

void dll_remove_last(DLL* head) {
  if(NULL != *head) {
    if(NULL == (*head)->next) {
      free(*head);
      *head = NULL;
    } else {
      DLL last = (*head)->next;
      while(NULL != last->next) {
	last = last->next;
      }
      DLL second_to_last = last->prev;
      free(last);
      second_to_last->next = NULL;
    }
  }
}

int main(void) {

  const int data = 5;

  DLL a = node_create(data);
  DLL b = node_create(data);

  a->next = b;
  b->prev = a;

  b = NULL;
  while(NULL != a) {
    dll_remove_last(&a);
  }

  _assert(NULL == a);
  
  return 0;
}
