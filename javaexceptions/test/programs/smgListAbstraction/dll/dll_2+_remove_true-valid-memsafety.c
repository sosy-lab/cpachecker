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

void dll_remove_first(DLL* head) {
  if(*head) {
    DLL second = (*head)->next;
    free(*head);
    if(second) {
      second->prev = NULL;
    }
    *head = second;
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
  while(NULL != a) {
    dll_remove_first(&a);
  }
  
  return 0;
}
