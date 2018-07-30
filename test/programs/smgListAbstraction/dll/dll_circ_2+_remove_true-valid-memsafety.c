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

node remove_head_from_circular_dll(node head) {
  if(NULL == head) {
    return NULL;
  } else if(head == head->next) {
    free(head);
    return NULL;
  } else {
    node second = head->next;
    node last = head->prev;
    second->prev = last;
    last->next = second;
    free(head);
    return second;
  }
}

int main(void) {

  node a = create_node();
  node b = create_node();
  a->data = 5;
  b->data = 5;
  a->next = b;
  b->prev = a;
  // add circular links
  a->prev = b;
  b->next = a;

  // remove external pointer
  b = NULL;

  while(NULL != a) {
    a = remove_head_from_circular_dll(a);
  }
  
  return 0;
}
