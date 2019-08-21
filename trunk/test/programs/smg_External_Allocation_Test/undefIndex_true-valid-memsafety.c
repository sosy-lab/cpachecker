struct Recursive {
  int s;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive ar[10];
  a = malloc(sizeof(struct Recursive));
  if (a->s < 10) {
    a->p = ar[a->s];
  }
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}

