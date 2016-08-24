extern __VERIFIER_nondet_int();

int main() {

  signed int a = __VERIFIER_nondet_int();
  signed int b = __VERIFIER_nondet_int();

  if (a > 10) {
    if (b >= 10) {
      a = b + 5;
      b--;

      if (b <= 9 && a <= 30) {

        b++;
        if (b != 10) {
ERROR:
          return -1;
        }
      }
    }
  }
}
