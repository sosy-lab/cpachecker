int main() {
  int i=0;
  for (i=0; i<10; i++) {
  }
  if (i != 0) {
    goto ERROR;
  }
  return 0;
ERROR:
  return -1;
}