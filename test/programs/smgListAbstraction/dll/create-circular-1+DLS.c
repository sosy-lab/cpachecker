#include <stdlib.h>

struct DLL {
  struct DLL *next;
  struct DLL *prev;
  int data;
};

typedef struct DLL *node;

node create_node() {
  node temp = (struct DLL *) malloc(sizeof(struct DLL));
  temp->next = NULL;
  temp->prev = NULL;
  temp->data = 0;
  return temp;
}

int main(void) {

  node a = create_node();
  a->data = 5;
  a->next = a;
  a->prev = a;

  while(NULL != a) {
    free(a);
    a = NULL;
  }
  
  return 0;
}
