#include "EqualNamedTypes.h"
#include <assert.h>
#include <stdlib.h>

int main(void) {
  struct Pair *p = malloc(sizeof(struct Pair));
  setFirst(p, 25);

  assert(p->a == 25);
  return p->a;
}
