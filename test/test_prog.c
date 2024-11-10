void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if (x < 0 || x % 2 == 0) { // Error Condition
          reach_error(); // Error
          return 1;
      }
  else return 0;
}

