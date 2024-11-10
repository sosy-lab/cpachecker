void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if (x % 2 == 0) {x = x/2;}
  int i = 3;
  while (i <= x) {
    if (x % i == 0) {
      x = x / i;
      i = 3;
      continue;
    }
    else {
      i = i + 2;
    }
  }
  if (x != 1) {
    reach_error();
    return 1; // error condition
  }
  else return 0;
}
