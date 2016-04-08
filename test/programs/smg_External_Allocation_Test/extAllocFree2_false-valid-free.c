int main() {
  int *a, *b;
  a = ext_allocation();
  b = a;
  free(a);
  free(b);
  return 0;
}

