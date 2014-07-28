int id(int x) {
  if (x==0) return 0;
  int ret = id(x-1) + 1;
  if (ret > 5) return 5;
  return ret;
}

void main() {
  int input;
  int result = id(input);
  if (result == 10) {
    ERROR: goto ERROR;
  }
}
