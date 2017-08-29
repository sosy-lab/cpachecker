int main() {
  short int x = 0;
  long long int y = 0;
  if ((x | y) == 0) {
ERROR:
    return 0;
  } else {
    return 1;
  }
}
