// example for pointer passing
#define  __attribute__(x) /*NOTHING*/
#include <stdlib.h>

typedef struct example_struct {
  void *data;
  size_t size;
} example_struct;

void init(example_struct *p) {
  p->data = NULL;
  p->size = 0;

  return;
}

int main(void) {
  example_struct p1;
  
  init(&p1);
  if (p1.data != NULL || p1.size != 0) {
    goto ERROR;
  } else {
    goto END;
  }

ERROR:
  return (1);

END:
  return (0);
}
