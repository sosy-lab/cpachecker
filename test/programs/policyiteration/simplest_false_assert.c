void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int i = 0;
  assert(i == 1);
}