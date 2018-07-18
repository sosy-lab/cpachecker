#include <stdlib.h>
extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

struct DLL {
  struct DLL* next;
  struct DLL* prev;
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
    // invalid memory leak
    create_node(-1);
  }
}

int main() {

  const int FIVE = 5;

  node* a = create_node(FIVE);
  node* b = create_node(FIVE);

  a->next = b;

  // remove external pointer
  b = NULL;

  while(__VERIFIER_nondet_int()) {
    node* temp = create_node(FIVE);
    temp->next = a;
    a = temp;
  }

  while(NULL != a) {
    node* temp = a->next;
    ASSERT(FIVE == a->data);
    free(a);
    a = temp;
  }

  return 0;
}
