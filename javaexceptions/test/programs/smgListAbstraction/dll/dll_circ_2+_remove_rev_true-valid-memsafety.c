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

DLL dll_circular_remove_last(DLL head) {
  if(NULL == head) {
    return NULL;
  } else {
    DLL last = head->prev;
    DLL second_to_last = last->prev;
    if(last == second_to_last) {
      head = NULL;
    } else {
      second_to_last->next = head;
      head->prev = second_to_last;
    }
    free(last);
    return head;
  }
}

int main(void) {

  const int data = 5;
  
  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  b->prev = a;

  // add circular links
  a->prev = b;
  b->next = a;

  b = NULL;
  while(NULL != a) {
    a = dll_circular_remove_last(a);
  }
  
  return 0;
}
