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
  a->p->s = 50;
  return 0;
}

int main() {
  foo();
  return 0;
}

