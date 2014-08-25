void f(int n) {
  if (n<3) return;
  n--;
  f2(n);
  __VERIFIER_error();
}

void f2(int n) {
  if (n<3) return;
  n--;
  f(n);
  __VERIFIER_error();
}

void main() {
  f(4);
}
