void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if (x > 10 || x < 0) return 0;
  while (x > 0) x -= 2;
  if (x == 0) return 0;
  reach_error();
}
