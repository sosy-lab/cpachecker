int sum(int n, int m) {
    if (n <= 0) {
      return m + n;
    } else {
      return sum(n - 1, m + 1);
    }
}

void main() {
  int a = 2;
  int b = 3;
  int result = sum(a, b);
  if (result != a + b) {
    __VERIFIER_error();
  }
}