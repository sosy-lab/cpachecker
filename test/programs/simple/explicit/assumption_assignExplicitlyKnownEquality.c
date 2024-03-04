extern int __VERIFIER_nondet_int();
int main() {
    int x = __VERIFIER_nondet_int();
    if (x == 5) {
    } else {
      x = 10;
    }
    if (x < 0) {
      goto ERROR;
    }
    return 0;
ERROR:
    return 1;

}
