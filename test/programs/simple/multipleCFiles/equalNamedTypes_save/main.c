#include "EqualNamedTypes.h"
#include <assert.h>
#include <stdlib.h>

struct sameNamed {
int a;
int b;
};

int main(void) {
  struct Pair *p = malloc(sizeof(struct Pair));
  setFirst(p, 25);

  assert(p->a == 25);

  struct sameNamed s = {.a=10, .b=100};
  s.b=10;

  assert(s.a == s.b);

  return p->a;
}
