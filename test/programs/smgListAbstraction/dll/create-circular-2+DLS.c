#include <stdlib.h>

struct DLL {
  struct DLL *next;
  struct DLL *prev;
  int data;
};

typedef struct DLL *node;

node create_node() {
  node temp = (struct DLL *) malloc(sizeof(struct DLL));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = 0;
  return temp;
}

void free_circular_DLL(node head) {
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
  b->data = 5;
  a->next = b;
  a->prev = b;
  b->next = a;
  b->prev = a;

  // remove external pointer
  b = NULL;
  
  free_circular_DLL(a);
    
  return 0;
}
