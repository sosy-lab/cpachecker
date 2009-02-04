int foo(int x) {
  int i;
  int y;

  for (i = 0; i < 10; i++) {
    y += x;
  }

  return y;
}

int main(void) {
  int __BLAST_NONDET_x;

  foo(__BLAST_NONDET_x);

  return (0);
}

