int foo(int x) {
  if (x) {
    return (x);
  }
  return 1;
}

int main() {
  int y = 0;
  y = foo(y);
  if (y) {
    ERROR:
    goto ERROR;
  } else {
    return 0;
  }
  return -1;
}
