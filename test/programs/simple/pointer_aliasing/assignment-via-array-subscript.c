void f(int *p) {
  p[0] = 1;
}
int main() {
  int i = 0;
  f(&i);
  if (i == 1) {
ERROR:
    return 1;
  }
  return 0;
}
