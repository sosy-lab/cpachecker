extern void abort();
void reach_error(){}


void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

int main() {
  int sum = 0;
  int *p = &sum;
  while(sum < 3) {
    sum++;
  }

  __VERIFIER_assert(sum == 3);
}
