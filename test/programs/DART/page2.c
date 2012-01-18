
int f(int x) { return 2 * x; }

int h(int x, int y) {
  if (x != y)
    if (f(x) == x + 10)
      abort();  /* error */
  return 0;
}

