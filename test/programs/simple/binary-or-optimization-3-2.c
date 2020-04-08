int main() {
  short int x = 1;
  long long int y = 0;
  if ((x | y) == 0) {
    return 1;
  } else {
ERROR:
    return 0;
  }
}
