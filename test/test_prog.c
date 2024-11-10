void reach_error(){}
extern int __VERIFIER_nondet_int();
int main() {
  int x = __VERIFIER_nondet_int();
  if ((x & 1) == 0) {x = x/2;}
  int i = 3;
  while (i <= x) {
    while (is_divisible(x,i)) {
            x = x / i;
    }
    i += 2;
  }
  if (x != 1) {
    reach_error();
    return 1; // error condition
  }
  else return 0;
}

inline int is_divisible(int a, int b) {
    return a % b == 0;
}
