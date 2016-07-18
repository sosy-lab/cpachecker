struct Recursive {
  int s;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive *b;
  a = ext_allocation();
  a->p->p = malloc(sizeof(struct Recursive));
  b = a->p;
  if (b->s > 100) {
    free(b->p);
  }
  if (100 > b->s) {
    free(a->p->p);
  }
  return 0;
}

int main() {
  foo();
  return 0;
}

