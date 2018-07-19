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

void remove_last_node_from_dll(node** head) {
  if(NULL != *head) {
    if(NULL == (*head)->next) {
      free(*head);
      *head = NULL;
    } else {
      node* last = (*head)->next;
      while(NULL != last->next) {
	last = last->next;
      }
      node* second_to_last = last->prev;
      free(last);
      second_to_last->next = NULL;
    }
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

  while(NULL != a) {
    remove_last_node_from_dll(&a);
  }

  ASSERT(NULL == a);
  
  return 0;
}
