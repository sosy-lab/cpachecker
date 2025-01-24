void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if (x % 2 == 0) {
    reach_error();
    return 1;
  }
  return 0;
}


