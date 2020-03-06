struct s1 {
  int i1;
  struct s2 {
    double d1;
    int i2;
  } s2;
};

int offset = __builtin_offsetof(struct s1, s2.i2);

int main() {
  if (offset != 12) {
ERROR:
    return 1;
  }
  return 0;
}
