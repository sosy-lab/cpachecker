struct Recursive {
  signed char sc;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive ar[128];
  a = malloc(sizeof(struct Recursive));
  if (a->sc >= 0) {
    a->p = ar[a->sc];
  }
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}

