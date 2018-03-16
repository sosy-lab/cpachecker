struct Recursive {
  unsigned char uc;
  struct Recursive *p;
  long q;
};


int foo() {
  struct Recursive *a;
  struct Recursive ar[255];
  a = malloc(sizeof(struct Recursive));
  a->p = ar[a->uc];
  free(a);
  return 0;
}

int main() {
  foo();
  return 0;
}

