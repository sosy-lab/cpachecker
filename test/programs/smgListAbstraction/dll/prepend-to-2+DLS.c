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
  node p = head;
  while(NULL != p) {
    node q = p->next;
    free(p);
    p = q;
  }
}

node prepend_to_dll(node head, int data) {
  node new_head = create_node();
  new_head->data = data;
  if(NULL != head) {
    new_head->next = head;
    head->prev = new_head;
  }
  return new_head;
}

int main(void) {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 5;
  a->next = b;
  b->prev = a;

  // remove external pointer
  b = NULL;

  a = prepend_to_dll(a, 7);
  
  free_dll(a);
  
  return 0;
}
