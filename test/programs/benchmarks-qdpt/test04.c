int foo(int x, int n) {
  int i;
  int y = 1;

  for (i = 0; i < n; i++) {
    y *= x;
  }

  return y;
}

int main(void) {
  int __BLAST_NONDET_x;
  int __BLAST_NONDET_y;

  foo(__BLAST_NONDET_x, __BLAST_NONDET_y);

  return (0);
}

