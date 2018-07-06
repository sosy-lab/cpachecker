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
  while(p != NULL) {
    node q = p->next;
    free(p);
    p = q;
  }
}

node append_to_dll(node head, int data) {
  node new_last = create_node();
  new_last->data = data;
  if(NULL == head) {
    return new_last;
  } else {
    node last = head;
    while(NULL != last->next) {
      last = last->next;
    }
    last->next = new_last;
    new_last->prev = last;
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

  a = append_to_dll(a, 7);

  free_dll(a);
  
  return 0;
}
