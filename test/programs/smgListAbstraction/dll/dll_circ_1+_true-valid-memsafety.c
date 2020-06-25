#include <stdlib.h>

typedef struct node {
  struct node *next;
  struct node *prev;
  int data;
} *DLL;

DLL create_node(int data) {
  DLL temp = (DLL) malloc(sizeof(struct node));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = data;
  return temp;
}

int main(void) {

  const int data = 5;
  
  DLL a = create_node(data);
  a->next = a;
  a->prev = a;

  while(NULL != a) {
    free(a);
    a = NULL;
  }
  
  return 0;
}
