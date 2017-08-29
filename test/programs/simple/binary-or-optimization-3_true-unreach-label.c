int main() {
  short int x = 1;
  long long int y = 0;
  if ((x | y) == 0) {
ERROR:
    return 1;
  } else {
    return 0;
  }
}
