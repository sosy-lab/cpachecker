
int f(int x, int y) {
  int z;
  z = y;
  if (x == z)
    if (y == x + 10)
      abort();
  return 0;
}

