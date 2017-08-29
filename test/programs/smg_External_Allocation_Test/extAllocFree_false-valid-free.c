int main() {
  int *a;
  a = ext_allocation();
  free(a);
  free(a);
  return 0;
}

