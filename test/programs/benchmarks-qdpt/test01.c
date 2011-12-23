int special_case(int x, int y);

int foo(int x, int y, int z) {
  if (z == 2) {
    special_case(y, x);
  }
  else {
    x = y;
  }

  return y;
}

int special_case(int x, int y) {
  // non-terminating loop
  while (y < x) {
    x--;
  }

  return 10;
}

int main(void) {
  int __BLAST_NONDET_x;
  int __BLAST_NONDET_y;
  int __BLAST_NONDET_z;

  foo(__BLAST_NONDET_x, __BLAST_NONDET_y, __BLAST_NONDET_z);

  return (0);
}

