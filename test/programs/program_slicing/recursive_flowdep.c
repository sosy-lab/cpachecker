
int fac(int n) {
  if (n <= 0) {
    __VERIFIER_error();
  }
  // misses statement
  // if (n == 1) return 1;
  return n + fac(n - 1);
}

int main() {
  int x = 4 + 6;
  fac(x);
}