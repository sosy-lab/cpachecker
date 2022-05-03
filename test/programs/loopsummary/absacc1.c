void reach_error() {};
int main() {
  int i = 0;
  int j = 42;
  int k = 42;
  while (k<1000000) {
    if (k%2==0) {
      i = i + 1;
    } else {
      j = j + 1;
    }
    i = i + 1;
    j = j + 1;
    k = k + 3;
  }
  int z = k-i-j;
  if (z) {
    reach_error();
  }
}
