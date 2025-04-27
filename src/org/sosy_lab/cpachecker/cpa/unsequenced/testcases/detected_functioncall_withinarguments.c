int g = 0;

int f1() {
  g = 2*g;
  return 5;
}

int f2() {
  //1. `g++` first **reads** `g`, and **creates a TMP** to save the original value.
  //2. `g++` then **writes** `g = g + 1`.
  //3. return TMP
  g++;
  return 7;
}

int foo(int a) {
  return 1;
}

int main() {
  int c = foo(f1()+f2());
  return 0;
}