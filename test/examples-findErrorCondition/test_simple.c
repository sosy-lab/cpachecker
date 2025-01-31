void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  int y = __VERIFIER_nondet_int();
  if (x > 5 || x < 0) return 0;
  if (y > 5 || y < 0) return 0;
  if (x + y == 5) {
   reach_error();
  }
}
