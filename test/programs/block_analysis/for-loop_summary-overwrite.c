extern int __VERIFIER_nondet_int();
void reach_error();

int main() {
    int x = __VERIFIER_nondet_int();
    if (x > 10) return;
    int y = 0;
    while (1) {
      y = __VERIFIER_nondet_int();
      if (y) y = 1;
      y++;
      if (y > 1) break;
      x -= x > 0 ? 5 : 0;
    }
    
    if (x > 10) reach_error();
    int n = __VERIFIER_nondet_int();
    if (n > 0 && n < 100) {
      n = n + n;
    } else {
      int n = 0;
    }
    if (x > 10) reach_error();

    if (x == 8) reach_error();
  return 0;
}
