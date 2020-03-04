struct s1 {
  int i1;
  struct s2 {
    double d1;
    int i2;
  } s2;
};

int offset = __builtin_offsetof(struct s1, i1);

int main() {
  if (offset != 16) {
ERROR:
    return 1;
  }
  return 0;
}
