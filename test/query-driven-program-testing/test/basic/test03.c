void foo(int x, int y) {
  while (x > y) {
    x--;
  }
}

int main(void) {
  int __BLAST_NONDET_x;
  int __BLAST_NONDET_y;

  foo(__BLAST_NONDET_x, __BLAST_NONDET_y);

  return (0);
}

