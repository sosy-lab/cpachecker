void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();

  // if (x < 0) { // Error Condition -> x is negative
  //  reach_error(); // Error State 1
  //  return 1;
  // }
  if (0 < x && x < 10) {
    reach_error();
    return 1;
  }
  int y = __VERIFIER_nondet_int();
  if (x - y == 0) {
    int z = __VERIFIER_nondet_int();
    y--;
    z++;
    if (y == z) {
      reach_error();
          return 1;
    }
  }


  return 0;
}

