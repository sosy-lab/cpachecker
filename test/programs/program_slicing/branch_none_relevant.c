int check(int i) {
  if (i == 0) {
    return 0;
  } else {
  ERROR:
    return 0;
  }
}

int main(void) {
  int i = 0;
  int c = 0;

  if (c == 0) {
    i += 1;
  } else {
    i += 2;
  }

  i = 0;
  int r = check(i);

  return r;
}
