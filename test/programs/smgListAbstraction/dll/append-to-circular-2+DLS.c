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
  if(NULL != head) {
    node* temp = head->next;
    while(temp != head) {
      ASSERT(expected == temp->data);
      temp = temp->next;
    }
    ASSERT(expected == head->data);
  }
}

void free_circular_dll(node* head) {
  if(NULL != head) {
    node* p = head->next;
    while(p != head) {
      node* q = p->next;
      free(p);
      p = q;
    }
    free(head);
  }
}

void append_to_circular_dll(node** head, int data) {
  node* new_last = create_node(data);
  if(NULL == *head) {
    new_last->prev = new_last;
    new_last->next = new_last;
    *head = new_last;
  } else {
    node* last = (*head)->prev;
    last->next = new_last;
    new_last->prev = last;
    new_last->next = *head;
    (*head)->prev = new_last;
  }
}

int main(void) {

  const int STORED_VALUE = 5;

  node* a = create_node(STORED_VALUE);
  node* b = create_node(STORED_VALUE);

  a->next = b;
  b->prev = a;

  a->prev = b;
  b->next = a;

  // remove external pointer
  b = NULL;

  append_to_circular_dll(&a, STORED_VALUE);

  check_data(a, STORED_VALUE);

  free_circular_dll(a);
  
  return 0;
}
