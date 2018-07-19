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

void free_dll(node head) {
  while(NULL != head) {
    node p = head->next;
    free(head);
    head = p;
  }
}

int main() {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 7;
  a->next = b;
  a->prev = NULL;
  b->next = NULL;
  b->prev = a;

  // remove external pointer
  b = NULL;

  free_dll(a);
    
  return 0;
}
