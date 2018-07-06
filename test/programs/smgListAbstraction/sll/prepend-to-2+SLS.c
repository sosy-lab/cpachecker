#include <stdlib.h>

struct SLL {
  struct SLL *next;
  int data;
};

typedef struct SLL *node;

node create_node() {
  node temp = (struct SLL *) malloc(sizeof(struct SLL));
  temp->next = NULL;
  temp->data = 0;
  return temp;
}

void free_sll(node head) {
  while(head != NULL) {
    node q = head->next;
    free(head);
    head = q;
  }
}

node prepend_to_sll(node head, int data) {
  node new_head = create_node();
  new_head->data = data;
  if(NULL != head) {
    new_head->next = head;
  }
  return new_head;
}

int main(void) {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 5;
  a->next = b;

  // remove external pointer
  b = NULL;

  a = prepend_to_sll(a, 6);
  
  free_sll(a);
  
  return 0;
}
