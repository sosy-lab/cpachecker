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

void ASSERT(int x) {
  if(!x) {
    // create memory leak
    create_node(-1);
  }
}

void check_data(node* head, int expected) {
  while(NULL != head) {
    node* temp = head->next;
    ASSERT(expected == head->data);
    head = temp;
  }
}

void prepend_to_dll(node** head, int data) {
  node* old_head = *head;
  *head = create_node(data);
  (*head)->next = old_head;
  old_head->prev = *head;
}

void free_dll(node* head) {
  while(NULL != head) {
    node* temp = head->next;
    free(head);
    head = temp;
  }
}

int main(void) {

  const int FIVE = 5;

  node* a = create_node(FIVE);
  node* b = create_node(FIVE);
  a->next = b;
  b->prev = a;

  // remove external pointer
  b = NULL;

  prepend_to_dll(&a, FIVE);

  check_data(a, FIVE);
  
  free_dll(a);

  return 0;
}
