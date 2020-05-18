extern void __VERIFIER_error();
void main() {
  int i = 0;
  int j = 0;
  FIRST:
  j = j + 1;
  SECOND:
  if (j < 4) {
    goto FIRST;
  } else {
    i = i + 1;
    if (i > 6) {
      ERROR:
      goto ERROR;
    } else {
      i = i - 1;
      goto SECOND;
    }
  }
}
