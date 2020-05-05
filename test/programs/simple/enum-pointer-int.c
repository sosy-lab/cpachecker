enum B {
  b = 1,
  c = 2
};

void func(int *arg) {
  *arg = c;
};

int main(void) {
  enum B var = b;

  func((int*)&var);
  if (var == 2) {
  ERROR:
    return 1;
  }
  return 0;
}
