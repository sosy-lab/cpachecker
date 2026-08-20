// Minimal example: DSS reports TRUE although reach_error() is reachable.
// Expected verdict: FALSE (the second reach_error() is unconditional).
// kInduction and bmc-incremental both report FALSE; DSS reports TRUE.
//
// reach_error() must have a body: only then does it become its own block that is
// entered by a call, which is what involves violation-condition propagation.

extern int __VERIFIER_nondet_int(void);
void reach_error(void) {}

int cmp2(int a, int b) {
  if (a < b) {
    return -1;
  }
  return 0;
}

int main(void) {
  int a = __VERIFIER_nondet_int();
  int ab = cmp2(a, 0);
  int ba = cmp2(0, a);
  if (ab < 0 && ba < 0) {
    reach_error();
  }
  reach_error();
  return 0;
}
