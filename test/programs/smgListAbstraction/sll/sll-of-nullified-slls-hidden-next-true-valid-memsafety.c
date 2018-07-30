#include <stdlib.h>

struct inner_sll {
  int data;
  struct inner_sll *next;
};

struct outer_sll {
  struct outer_sll *next;
  struct inner_sll *inner;
};

typedef struct outer_sll outer_node;
typedef struct inner_sll inner_node;

outer_node* create_outer_node() {
  outer_node* temp = (outer_node *) malloc(sizeof(outer_node));
  temp->next = NULL;
  temp->inner = NULL;
  return temp;
}

inner_node* create_inner_node(int data) {
  inner_node* temp = (inner_node *) malloc(sizeof(inner_node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void free_hierarchical_SLL(outer_node* head) {
  while(NULL != head) {
    inner_node* inner_A = head->inner;
    outer_node* outer_B = head->next;
    while(NULL != inner_A) {
      inner_node* inner_B = inner_A->next;
      free(inner_A);
      inner_A = inner_B;
    }
    free(head);
    head = outer_B;
  }
}

void ASSERT(int x) {
  if(!x) {
    // create memory leak
    create_node(-1);
  }
}

void check_data(outer_node* head, int data) {
  while(NULL != head) {
    inner_node* inner = head->inner;
    while(NULL != inner) {
      ASSERT(data == inner->data);
      inner = inner->next;
    }
    head = head->next;
  }
}

int main(void) {

  const int STORED_VALUE = 0;

  outer_node* a = create_outer_node();
  outer_node* b = create_outer_node();

  inner_node* a_0 = create_inner_node(STORED_VALUE);
  inner_node* a_1 = create_inner_node(STORED_VALUE);
  inner_node* b_0 = create_inner_node(STORED_VALUE);
  inner_node* b_1 = create_inner_node(STORED_VALUE);

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

  check_data(a, STORED_VALUE);

  free_hierarchical_SLL(a);

  return 0;
}
