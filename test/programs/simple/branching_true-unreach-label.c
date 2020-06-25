void main() {
  int i = 1;
  if (!i) {
    int *p = &i;
    (*p)++;
  } else {
  }
  if (i != 1) {
ERROR:
    return;
  }
}
