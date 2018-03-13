void assert(int cond) { if (!cond) { ERROR: return; } }

int main() {
  int i = 0;

  while (1) {
    if (i >=0 && i <= 5) {
      i -= 1;
    } else if (i <= -1) {
      i += 7;
    } else if (i == 6) {
      i = 100;
      break;
    } else {
      i = 1000;
      break;
    }
  }

  assert(i == 100);
}
