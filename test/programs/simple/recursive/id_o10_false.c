int id(int x) {
  if (x==0) return 0;
  return id(x-1) + 1;
}

void main() {
  int input;
  int result = id(input);
  if (result == 10) {
    ERROR: goto ERROR;
  }
}
