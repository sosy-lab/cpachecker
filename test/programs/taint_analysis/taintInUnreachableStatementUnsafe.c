extern char __VERIFIER_nondet_int();

// This program is unsafe (false-positive) for a path-insensitive taint analysis
int main() {
    int x, y;
    x = 1;
    y = 2;

    while (x > 5) {
      // unreachable statements
      y = __VERIFIER_nondet_int();
      x--;
    }

    __VERIFIER_is_public(y, 0);
}
