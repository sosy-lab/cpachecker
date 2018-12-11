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
    DLL temp = head->next;
    free(head);
    head = temp;
  }
}

int main() {

  const int data_1 = 5;
  const int data_2 = 7;

  DLL a = node_create(data_1);
  DLL b = node_create(data_2);
  a->next = b;
  b->prev = a;

  b = NULL;
  dll_destroy(a);
    
  return 0;
}
