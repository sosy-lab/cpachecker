#include <stdlib.h>

struct DLL {
  struct DLL *next;
  struct DLL *prev;
  int data;
};

typedef struct DLL node;

node create_node(int data) {
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
  while(head != NULL) {
    ASSERT(expected == head->data);
    head = head->next;
  }
}

void free_dll(node* head) {
  while(head != NULL) {
    node* temp = head->next;
    free(head);
    head = temp;
  }
}

void append_to_dll(node** head, int data) {
  node* new_last = create_node(data);
  if(NULL == *head) {
    *head = new_last;
  } else {
    node* last = *head;
    while(NULL != last->next) {
      last = last->next;
    }
    last->next = new_last;
    new_last->prev = last;
  }
}

int main(void) {

  const int STORED_VALUE = 5;

  node* a = create_node(STORED_VALUE);
  node* b = create_node(STORED_VALUE);

  a->next = b;
  b->prev = a;

  // remove external pointer
  b = NULL;

  append_to_dll(&a, 7);

  // next line should fail!
  check_data(a, STORED_VALUE);

  free_dll(a);
  
  return 0;
}
