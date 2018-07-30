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

void update_sll_node(node head, int idx, int data) {
  if(NULL == head) {
    return;
  } else {
    node p = head;
    while(NULL != p && idx > 0) {
      p = p->next;
      --idx;
    }
    if(NULL != p) {
      p->data = data;
    }
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

  int i = 0;
  while(i < 2) {
    update_sll_node(a, i, 7);
    ++i;
  }

  free(a->next);
  free(a);
  
  return 0;
}
