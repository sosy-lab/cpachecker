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
  node p = head->next;
  while(p != head) {
    node q = p->next;
    free(p);
    p = q;
  }
  free(head);
}

node prepend_to_cyclic_sll(node head, int data) {
  node new_head = create_node();
  new_head->data = data;
  new_head->next = head;
  if(NULL != head) {
    node last = head;
    while(head != last->next) {
      last = last->next;
    }
    last->next = new_head;
  }
  return new_head;
}

int main(void) {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 5;
  a->next = b;
  b->next = a;

  // remove external pointer
  b = NULL;

  a = prepend_to_cyclic_sll(a, 7);

  free_sll(a);
  
  return 0;
}
