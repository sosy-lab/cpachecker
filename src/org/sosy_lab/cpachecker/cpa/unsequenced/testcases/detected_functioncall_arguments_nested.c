int g = 0;

int f1(int a) {
  return a;
}

int f2() {
  g = 3;
  return 7;
}

int f3(int a) {
  g = 4;
  return 7;
}

int main() {
  int c =  f2() + f1(f3(g));
  return 0;
}