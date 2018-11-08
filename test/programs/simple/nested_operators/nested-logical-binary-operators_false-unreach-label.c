void f(int value, int expect) {
  if (value != expect) {
  ERROR:
    goto ERROR;
  }
}

int main() {
  int a = 4, b = 2, d = 0;
  f(d = a && b, 0);
  f(d = a || b, 0);
  return 0;
}
