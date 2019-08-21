void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int i = 1;

  assert(i == 1);
}
