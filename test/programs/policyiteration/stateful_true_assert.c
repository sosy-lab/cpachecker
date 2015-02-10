#include <assert.h>
#include <stdbool.h>

int main() {
  int i = 0;
  int a = 1;

  while (true) {
    if (i >=0 && i <= 5) {
      i -= 1;
    } else if (i <= -1) {
      i += 7;
    } else if (a == 1 && i == 6) {
      i = 100;
      break;
    } else {
      i = 1000;
      break;
    }
  }

  assert(i == 100);
}
