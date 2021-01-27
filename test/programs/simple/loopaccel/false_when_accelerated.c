void reach_error() {};
void __VERIFIER_assert(int cond) {
  if (!cond) {
    reach_error();
  }
}

int main() {
  int i = 0;
  while (i < 100) {
    i = i + 11;
  }
  __VERIFIER_assert(i==110);
}
