int foo() {
  int *a;
  a = ext_allocation();
  return 0;
}

int main() {
  foo();
  return 0;
}

