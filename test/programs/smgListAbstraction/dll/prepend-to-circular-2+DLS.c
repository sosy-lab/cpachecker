#include <stdlib.h>

struct DLL {
  struct DLL *next;
  struct DLL *prev;
  int data;
};

typedef struct DLL node;

node* create_node(int data) {
  node* temp = (node *) malloc(sizeof(node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

void free_dll(node* head) {
  node* p = head->next;
  while(p != head) {
    node* q = p->next;
    free(p);
    p = q;
  }
  free(head);
}

void ASSERT(int x) {
  if(!x) {
    // create memory leak
    create_node(-1);
  }
}

void check_data(node* head, int expected) {
  while(head != head->next) {
    node* temp = head->next;
    ASSERT(expected == head->data);
    head = temp;
  }
}

void prepend_to_circular_dll(node** head, int data) {
  node* old_head = *head;
  if(NULL == old_head) {
    old_head->next = old_head;
    old_head->prev = old_head;
    *head = old_head;
  } else {
    node* last = old_head->prev;
    *head = create_node(data);
    (*head)->next = old_head;
    old_head->prev = *head;
    last->next = *head;
    (*head)->prev = last;
  }
}

int main(void) {

  const int FIVE = 5;

  node* a = create_node(FIVE);
  node* b = create_node(FIVE);

  a->next = b;
  b->prev = a;

  // add circular links
  a->prev = b;
  b->next = a;
  
  // remove external pointer
  b = NULL;

  prepend_to_circular_dll(a, FIVE);

  // expected to fail!
  check_data(a, 7);

  free_dll(a);
  
  return 0;
}
