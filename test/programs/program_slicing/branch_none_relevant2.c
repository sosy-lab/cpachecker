int main(void) {
  int i = 0;
  int c = 0;

  if (c == 0) {
    i += 1;
  } else {
    i += 2;
  }

  i = 0;
  if (i >= 0) {
ERROR:
    return -1;
  }

  return 0;
}
