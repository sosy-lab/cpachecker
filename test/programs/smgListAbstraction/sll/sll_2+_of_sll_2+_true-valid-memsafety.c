// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

typedef struct inner_node {
  struct inner_node* next;
  int data;
} *SLL_inner;

typedef struct outer_node {
  struct outer_node* next;
  struct inner_node* inner;
} *SLL_outer;

SLL_outer node_outer_create() {
  SLL_outer temp = (SLL_outer) malloc(sizeof(struct outer_node));
  temp->next = NULL;
  temp->inner = NULL;
  return temp;
}

SLL_inner node_inner_create(int data) {
  SLL_inner temp = (SLL_inner) malloc(sizeof(struct inner_node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void _assert(int x) {
  if(!x) {
    node_inner_create(-1);
  }
}

void sll_hierarchical_check_and_destroy(SLL_outer head, int expected) {
  SLL_outer p = head;
  while(p) {
    SLL_inner p_inner = p->inner;
    SLL_outer q = p->next;
    while(p_inner) {
      SLL_inner q_inner = p_inner->next;
      _assert(p_inner->data);
      free(p_inner);
      p_inner = q_inner;
    }
    free(p);
    p = q;
  }
}

int main(void) {

  const int data = 5;
  
  SLL_outer a = node_outer_create();
  SLL_outer b = node_outer_create();

  SLL_inner a_0 = node_inner_create(data);
  SLL_inner a_1 = node_inner_create(data);
  SLL_inner b_0 = node_inner_create(data);
  SLL_inner b_1 = node_inner_create(data);
  
  a->inner = a_0;
  a_0->next = a_1;
  b->inner = b_0;
  b_0->next = b_1;
  
  a->next = b;

  a_0 = NULL;
  a_1 = NULL;
  b_0 = NULL;
  b_1 = NULL;
  b = NULL;
  sll_hierarchical_check_and_destroy(a, data);
  
  return 0;
}
