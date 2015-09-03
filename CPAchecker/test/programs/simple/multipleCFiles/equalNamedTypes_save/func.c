#include "EqualNamedTypes.h"

struct sameNamed {
int x;
};


void setFirst(struct Pair *p, int val) {
  struct sameNamed str = {.x = 5};
  p->a = str.x;
}
