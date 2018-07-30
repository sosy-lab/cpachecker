#include <stdlib.h>

struct SLL {
  struct SLL *next;
  int data;
};

typedef struct SLL node;

int len(node* head) {
  int l = 0;
  while(NULL != head) {
    l++;
    head = head->next;
  }
  return l;
}

void append_to_sll(node** head, int data) {
  node* new_node = create_node(data);
  if(NULL == *head) {
    *head = new_node;
  } else {
    node* temp = *head;
    while(NULL != temp->next) {
      temp = temp->next;
    }
    temp->next = new_node;
  }
}

void free_sll(node* head) {
  while(NULL != head) {
    node* temp = head->next;
    free(head);
    head = temp;
  }
}

void ASSERT(int x) {
  if(!x) {
    // create memory leak
    create_node(-1);
  }
}

node* create_node(int data) {
  node* temp = (node *) malloc(sizeof(node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

int main(void) {
  const int STORED_VALUE = 1;
  const int LIST_LENGTH = 5;

  node* lst = NULL;

  int i = 0;
  while(i < LIST_LENGTH) {
    append_to_sll(&lst, STORED_VALUE);
    ++i;
  }

  ASSERT(LIST_LENGTH == len(lst));

  free_sll(lst);

  return 0;
}
