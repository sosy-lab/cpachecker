int main() {
  void *p = 0;
  void *q = -1;

  // GCC implements pointer comparisons as unsigned, so this branch is taken
  if (q > p) {
    goto ERROR;
  }
  return 0;

ERROR:
  return 1;
}
