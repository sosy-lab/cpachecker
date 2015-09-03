#include <stdlib.h>
#include <stdbool.h>

extern bool __VERIFIER_nondet_bool();

int main() {
  bool selector = __VERIFIER_nondet_bool();

  int *ptr;

  if (!selector) {
    if (selector) {
      *ptr = 666;
    }
    ptr = malloc(sizeof(int));
    free(ptr);
  }

  return 0;
}
