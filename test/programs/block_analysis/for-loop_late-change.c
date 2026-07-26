extern int __VERIFIER_nondet_int();
void reach_error(){}

int main() {
  int x = __VERIFIER_nondet_int();
  if (x > 0 && x < 100) {
    for (int i = 0; i < 7; i++) {
      x++;
      if (i == 6) x--;
    }
  } else {
    x = -1;
  }
  if (x >= 105) reach_error();
}
