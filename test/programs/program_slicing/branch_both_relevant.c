int check(int i) {
  if (i == 2) {
    return 0;
  } else {
  ERROR:
    return 1;
  }
}

int main(void) {
  int i = 0;
  int c = 0;

  for (int j = 0; j < 2; j++) {
    if (c == j) {
      i++;
    } else {
      i++;
    }
  }

  return check(i);
}
