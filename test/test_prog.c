void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  // if (x < 0) { // Error Condition -> x is negative
  //  reach_error(); // Error State 1
  //  return 1;
  // }
  // if (0 < x < 10) { // Error Condition -> x is negative
  //     reach_error(); // Error State 1
  //    return 1;
  // }
  if (x == 2) { // Error Condition -> x is even
    reach_error(); // Error State 2
    return 1;
  }

  return 0;
}

