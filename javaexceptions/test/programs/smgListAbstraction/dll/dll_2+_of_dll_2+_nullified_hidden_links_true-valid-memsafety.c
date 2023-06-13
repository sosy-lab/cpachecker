// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

typedef struct inner_node {
  int data;
  struct inner_node* next;
  struct inner_node* prev;
} *DLL_inner;

typedef struct outer_node {
  struct outer_node* next;
  struct outer_node* prev;
  struct inner_node* inner;
} *DLL_outer;

DLL_outer outer_node_create() {
  DLL_outer temp = (DLL_outer) malloc(sizeof(struct outer_node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->inner = NULL;
  return temp;
}

DLL_inner inner_node_create(int data) {
  DLL_inner temp = (DLL_inner) malloc(sizeof(struct inner_node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

void dll_hierarchical_destroy(DLL_outer head) {
  while(head) {
    DLL_inner inner_A = head->inner;
    DLL_outer outer_B = head->next;
    while(inner_A) {
      DLL_inner inner_B = inner_A->next;
      free(inner_A);
      inner_A = inner_B;
    }
    free(head);
    head = outer_B;
  }
}

void _assert(int x) {
  if(!x) {
    // create memory leak
    inner_node_create(-1);
  }
}

void dll_check_data(DLL_outer head, int data) {
  while(head) {
    DLL_inner inner = head->inner;
    while(NULL != inner) {
      _assert(data == inner->data);
      inner = inner->next;
    }
    head = head->next;
  }
}

int main(void) {

  const int data = 0;

  DLL_outer a = outer_node_create();
  DLL_outer b = outer_node_create();

  DLL_inner a_0 = inner_node_create(data);
  DLL_inner a_1 = inner_node_create(data);
  DLL_inner b_0 = inner_node_create(data);
  DLL_inner b_1 = inner_node_create(data);

  // connect inner nodes
  a->inner = a_0;
  a_0->next = a_1;
  a_1->prev = a_0;
  b->inner = b_0;
  b_0->next = b_1;
  b_1->prev = b_0;

  // connect outer nodes
  a->next = b;
  b->prev = a;

  // remove external pointers
  a_0 = NULL;
  a_1 = NULL;
  b_0 = NULL;
  b_1 = NULL;
  b = NULL;

  dll_check_data(a, data);

  dll_hierarchical_destroy(a);

  return 0;
}
