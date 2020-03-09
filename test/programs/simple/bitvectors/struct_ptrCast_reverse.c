#include <stdlib.h>
#include <stdio.h>

struct stub {
    void* ptr;
    int   value;
};


int main() {
  struct stub top;
  struct stub inner;
  struct stub* tmp;
  tmp = &inner;
  top.ptr = tmp;
  inner.value = 2;
  
  if (((struct stub*)(top.ptr))->value != 2) {
    goto ERROR;
  }
  printf ("SAFE\n");
  return 0;

ERROR:
  printf ("UNSAFE\n");
ERROR2:
  goto ERROR2;

  return 1;
}
