void assert(int cond) { if (!cond) { ERROR: return; } }

const int BOUND = 10;

int inc(int input) {
  return input + 1;
}

int main() {
  int sum = 0;
  int i;
  for (i=0; i<BOUND; i++) {
    sum = inc(sum);
  }
  assert(sum == BOUND);
  assert(sum == i);
  return 0;
}
