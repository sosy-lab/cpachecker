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

void free_circular_SLL(node head) {
  node second = head->next;
  while(second != head) {
    node third = second->next;
    free(second);
    second = third;
  }
  free(head);
}

int main() {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 7;
  a->next = b;
  b->next = a;

  // remove external pointer
  b = NULL;

  free_circular_SLL(a);
    
  return 0;
}
