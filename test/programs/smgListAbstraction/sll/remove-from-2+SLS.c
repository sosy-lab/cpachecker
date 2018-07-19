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

node remove_first_node_of_sll(node head) {
  if(NULL == head) {
    return NULL;
  } else if(NULL == head->next) {
    free(head);
    return NULL;
  } else {
    node second = head->next;
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

  // remove external pointer
  b = NULL;

  while(NULL != a) {
    a = remove_first_node_of_sll(a);
  }
    
  return 0;
}
