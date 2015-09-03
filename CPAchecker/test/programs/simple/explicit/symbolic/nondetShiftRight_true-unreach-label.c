extern __VERIFIER_nondet_char();

int main() {
  unsigned char a = __VERIFIER_nondet_char();
  unsigned char b = __VERIFIER_nondet_char();

  a = a | 1;
  b = b | 1;
  a = a << 7;
  b = b << 7;

  if (a == 0) {
    goto ERROR;
  }

  if (a != b) {
ERROR:
    return -1;
  }
}
