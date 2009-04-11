int foo() {
  return 10;
}

int foo2(int x) {
  return ((++x) + 10);
}

int main(void) {
  int x;

  x = foo();

  x = foo2(100);

  return (x);
}

