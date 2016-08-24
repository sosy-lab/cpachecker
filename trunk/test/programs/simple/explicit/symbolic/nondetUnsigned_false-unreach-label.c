extern __VERIFIER_nondet_char();

int main() {
  signed char b = __VERIFIER_nondet_char();

  if (b < 0) {
    b = -b;
  }

  unsigned char a = b;

  // set msb to 1;
  b = b | 128;
  a = a | 128;

  if (a != b) {
ERROR:
    return -1;

  } else {
    return 0;
  }
}
