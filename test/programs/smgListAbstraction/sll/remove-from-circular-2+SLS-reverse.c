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

node remove_last_node_from_sll(node head) {
  if(NULL == head) {
    return NULL;
  } else if(head == head->next) {
    free(head);
    return NULL;
  } else {
    node last = head->next;
    node second_to_last = head;
    while(head != last->next) {
      second_to_last = last;
      last = last->next;
    }
    free(last);
    second_to_last->next = head;
    return head;
  }
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

  while(NULL != a) {
    a = remove_last_node_from_sll(a);
  }
  
  return 0;
}
