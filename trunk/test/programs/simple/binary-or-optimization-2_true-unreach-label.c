int main() {
  short int x = 0;
  short int y = 1;
  if ((x | y) == 0) {
ERROR:
    return 1;
  } else {
    return 0;
  }
}
