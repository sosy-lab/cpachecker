void __VERIFIER_error();
void __VERIFIER_assume(int cond);

int main() {
  int sum = 0;
  int prod = 1;

  int i = 1;
  while (i < 10) {
    sum = sum + i;
    prod = prod * i;
    i = i + 1;
  }
  // unreachable error
  if (!(sum >= 0)) goto ERROR;
  // reachable error
  if (!(prod < 10)) goto ERROR;

  return 0;
ERROR:
  __VERIFIER_error();
  return -1;
}
