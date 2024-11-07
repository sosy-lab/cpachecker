void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  if (x + y < 0) {
    reach_error();
    return 1; // error condition
  }
  return 0;
}
