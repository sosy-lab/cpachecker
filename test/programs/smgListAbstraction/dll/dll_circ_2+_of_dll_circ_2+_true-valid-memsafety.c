// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

typedef struct inner_node {
  struct inner_node *next;
  struct inner_node *prev;
  int data;
} *DLL_inner;

typedef struct outer_node {
  struct outer_node *next;
  struct outer_node *prev;
  struct inner_node *inner;
} *DLL_outer;

DLL_outer node_outer_create() {
  DLL_outer temp = (DLL_outer) malloc(sizeof(struct outer_node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->inner = NULL;
  return temp;
}

DLL_inner node_inner_create(int data) {
  DLL_inner temp = (DLL_inner) malloc(sizeof(struct inner_node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

void dll_hierarchical_destroy(DLL_outer head) {
  DLL_outer p = head->next;
  while(head != p) {
    DLL_inner p_inner = p->inner;
    DLL_inner p_inner_next = p_inner->next;
    while(p_inner != p_inner_next) {
      DLL_inner q_inner_next = p_inner_next->next;
      free(p_inner_next);
      p_inner_next = q_inner_next;
    }
    free(p_inner);
    DLL_outer q = p->next;
    free(p);
    p = q;
  }
  DLL_inner p_inner = head->inner;
  DLL_inner p_inner_next = p_inner->next;
  while(p_inner != p_inner_next) {
    DLL_inner q_inner_next = p_inner_next->next;
    free(p_inner_next);
    p_inner_next = q_inner_next;
  }
  free(p_inner);
  free(head);
}

int main(void) {

  const int data = 5;
  
  DLL_outer a = node_outer_create();
  DLL_outer b = node_outer_create();
  
  DLL_inner a_0 = node_inner_create(data);
  DLL_inner a_1 = node_inner_create(data);
  DLL_inner b_0 = node_inner_create(data);
  DLL_inner b_1 = node_inner_create(data);
  
  // connect inner nodes
  a->inner = a_0;
  a_0->next = a_1;
  a_0->prev = a_1;
  a_1->next = a_0;
  a_1->prev = a_0;
  b->inner = b_0;
  b_0->next = b_1;
  b_0->prev = b_1;
  b_1->next = b_0;
  b_1->prev = b_0;
  
  // connect outer nodes
  a->next = b;
  a->prev = b;
  b->next = a;
  b->prev = a;

  // remove external pointers
  a_0 = NULL;
  a_1 = NULL;
  b_0 = NULL;
  b_1 = NULL;
  b = NULL;
  dll_hierarchical_destroy(a);
  
  return 0;
}
