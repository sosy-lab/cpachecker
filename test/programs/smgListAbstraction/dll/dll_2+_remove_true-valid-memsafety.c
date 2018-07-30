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

node remove_first_node_of_dll(node head) {
  if(NULL == head) {
    return NULL;
  } else {
    node second = head->next;
    free(head);
    if(NULL != second) {
      second->prev = NULL;
    }
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

  // remove external pointer
  b = NULL;

  while(NULL != a) {
    a = remove_first_node_of_dll(a);
  }
  
  return 0;
}
