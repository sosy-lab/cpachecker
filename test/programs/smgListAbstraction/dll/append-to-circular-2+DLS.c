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
  node p = head->next;
  while(p != head) {
    node q = p->next;
    free(p);
    p = q;
  }
  free(head);
}

node append_to_circular_dll(node head, int data) {
  node new_last = create_node();
  new_last->data = data;
  if(NULL == head) {
    new_last->prev = new_last;
    new_last->next = new_last;
    return new_last;
  } else {
    node last = head->prev;
    last->next = new_last;
    new_last->prev = last;
    new_last->next = head;
    head->prev = new_last;
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
  // add circular links
  a->prev = b;
  b->next = a;

  // remove external pointer
  b = NULL;

  a = append_to_circular_dll(a, 7);

  free_dll(a);
  
  return 0;
}
