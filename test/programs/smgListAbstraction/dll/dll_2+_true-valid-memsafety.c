#include <stdlib.h>

typedef struct node {
  struct node* next;
  struct node* prev;
  int data;
} *DLL;

DLL node_create(int data) {
  DLL temp = (DLL) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

void dll_destroy(DLL head) {
  while(head) {
    DLL p = head->next;
    free(head);
    head = p;
  }
}

int main() {

  const int data = 5;
  
  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  b->prev = a;

  b = NULL;
  dll_destroy(a);
    
  return 0;
}
