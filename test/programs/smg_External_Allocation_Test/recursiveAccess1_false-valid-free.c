struct Recursive {
  struct Recursive *p;
};

int foo() {
  struct Recursive *a;
  struct Recursive *b;
  a = ext_allocation();
  b = a->p->p;
  free(b->p);
  free(a->p->p->p);
  return 0;
}

int main() {
  foo();
  return 0;
}

