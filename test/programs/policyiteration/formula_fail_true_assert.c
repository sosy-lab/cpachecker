extern int __VERIFIER_nondet_int();

int main() {
    int x = 0;
    while (__VERIFIER_nondet_int()) {
        x = 2;
    }
    if (!(x <= 2)) {
ERROR:
      return 1;
    }
}
