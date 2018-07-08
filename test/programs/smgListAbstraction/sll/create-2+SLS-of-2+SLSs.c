#include <stdlib.h>

struct inner_sll {
  struct inner_sll *next;
  int data;
};

struct outer_sll {
  struct outer_sll *next;
  struct inner_sll *inner;
};

typedef struct outer_sll *outer_node;
typedef struct inner_sll *inner_node;

outer_node create_outer_node() {
  outer_node temp = (struct outer_sll *) malloc(sizeof(struct outer_sll));
  temp->next = NULL;
  temp->inner = NULL;
  return temp;
}

inner_node create_inner_node() {
  inner_node temp = (struct inner_sll *) malloc(sizeof(struct inner_sll));
  temp->next = NULL;
  temp->data = 0;
  return temp;
}

void free_hierarchical_SLL(outer_node head) {
  outer_node p = head;
  while(NULL != p) {
    inner_node p_inner = p->inner;
    outer_node q = p->next;
    while(NULL != p_inner) {
      inner_node q_inner = p_inner->next;
      free(p_inner);
      p_inner = q_inner;
    }
    free(p);
    p = q;
  }
}

int main(void) {

  outer_node a = create_outer_node();
  outer_node b = create_outer_node();
  
  inner_node a_0 = create_inner_node();
  inner_node a_1 = create_inner_node();
  inner_node b_0 = create_inner_node();
  inner_node b_1 = create_inner_node();
  a_0->data = 5;
  a_1->data = 5;
  b_0->data = 5;
  b_1->data = 5;
  
  // connect inner nodes
  a->inner = a_0;
  a_0->next = a_1;
  a_1->next = NULL;
  b->inner = b_0;
  b_0->next = b_1;
  b_1->next = NULL;
  
  // connect outer nodes
  a->next = b;

  // remove external pointers
  a_0 = NULL;
  a_1 = NULL;
  b_0 = NULL;
  b_1 = NULL;
  b = NULL;

  free_hierarchical_SLL(a);
  
  return 0;
}
