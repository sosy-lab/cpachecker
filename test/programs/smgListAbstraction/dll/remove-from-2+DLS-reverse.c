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

node remove_last_node_from_dll(node head) {
  if(NULL == head) {
    return NULL;
  } else {
    node last = head->next;
    while(NULL != last->next) {
      last = last->next;
    }
    node second_to_last = last->prev;
    free(last);
    second_to_last->next = NULL;
    return head;
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
    a = remove_last_node_from_dll(a);
  }
  
  return 0;
}
