#include <stdlib.h>
#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();

int main() {
  bool selector = __VERIFIER_nondet_bool();

  int *ptr = NULL;

  if (!selector) {
    ptr = malloc(sizeof(int));
  }

  if (selector) {
    *ptr = 666;
  }
  free(ptr);

  return 0;
}
