int foo(int x, int n) {
  int i;
  int y = 1;

  for (i = 0; i < n; i++) {
    y *= x;
  }

  return y;
}

