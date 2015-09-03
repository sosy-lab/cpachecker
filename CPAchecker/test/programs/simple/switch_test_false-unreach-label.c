int main() {
  int a = 1, c = 0, d = 0;

  switch (a) {
    case 1:
      c++;
      break;
    default:
      d++;
      break;
  }

  switch (a) {
    default:
      d++;
      break;
    case 1:
      c++;
      break;
  }

  switch (a) {
    case 1:
      c++;
      break;
  }

  switch (a) {
    default:
      d++;
      break;
  }

  switch (a) {
    case 1:
      c++;
    default:
      d++;
  }

  switch (a) {
    default:
      d++;
    case 1:
      c++;
  }

  goto L;
  switch (a) {
    case 1:
      c++;
      break;
    L:
    default:
      d++;
  }

  if (c == 5 && d == 3) {
ERROR:
    return 1;
  }
  return 0;
}
