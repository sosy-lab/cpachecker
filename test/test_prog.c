void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if (x > 1 && x < 6) {
    reach_error();
    return 1; // error condition
  }
  return 0;
}
