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

void update_dll_node(node head, int idx, int data) {
  if(NULL == head) {
    return;
  } else {
    node p = head;
    while(head != p->next && idx > 0) {
      p = p->next;
      --idx;
    }
    if(0 == idx) {
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
  b->prev = a;

  // add circular links
  a->prev = b;
  b->next = a;

  // remove external pointer
  b = NULL;
  
  int i = 1;
  while(i > -1) {
    update_dll_node(a, i, 7);
    --i;
  }
  
  free(a->next);
  free(a);

  return 0;
}
