void f(int n) {
  if (n<3) return;
  n--;
  f(n);
  __VERIFIER_error();
}

void main() {
  f(4);
}
