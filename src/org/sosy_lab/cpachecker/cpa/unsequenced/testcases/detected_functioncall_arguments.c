int g = 0;

int f1() {
  g = 2*g;
  return 5;
}

int f2() {
  ++g;
  return 7;
}

int foo(int a, int b) {
  return 1;
}

int main() {
  int c = foo(f1(), f2());
  return 0;
}