void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int x = 0;
  int y = 0;
  while (1) {
    x++;
    y++;
    x += y;
    y += x;

    if (x >= 10 || y >= 10) break;
  }

  assert(y >= 10);
}
