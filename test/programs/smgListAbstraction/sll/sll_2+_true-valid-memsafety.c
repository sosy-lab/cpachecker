#include <stdlib.h>

typedef struct node {
  struct node* next;
  int data;
} *SLL;

SLL node_create(int data) {
  SLL temp = (node *) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->data = data;
  return temp;
}

void sll_destroy(SLL head) {
  while(head) {
    SLL temp = head->next;
    free(head);
    head = temp;
  }
}

int main(void) {

  const int data = 5;

  SLL a = node_create(data);
  SLL b = node_create(data);
  a->next = b;

  b = NULL;
  sll_destroy(a);
    
  return 0;
}
