extern __VERIFIER_nondet_int();

int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();

  if ((x < y + 2) && (x > y - 1)) {
    if ((x - 1 < y) == 1) {
      if ((x == y) != 1) {
ERROR:
        return -1;
      }
    }
  }
}
