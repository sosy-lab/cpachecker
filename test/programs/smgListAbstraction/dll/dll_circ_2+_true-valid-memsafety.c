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

void dll_circular_destroy(DLL head) {
  DLL second = head->next;
  while(second != head) {
    DLL third = second->next;
    free(second);
    second = third;
  }
  free(head);
}

int main() {

  const int data = 5;
  
  DLL a = node_create(data);
  DLL b = node_create(data);
  a->next = b;
  a->prev = b;
  b->next = a;
  b->prev = a;

  b = NULL;
  dll_circular_destroy(a);
    
  return 0;
}
