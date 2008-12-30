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

