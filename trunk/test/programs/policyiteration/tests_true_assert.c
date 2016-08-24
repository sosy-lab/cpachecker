#include <assert.h>
#include <stdbool.h>

int main() {
  int x = 0;
  int y = 0;
  while (true) {
    x++;
    y++;
    x += y;
    y += x;

    if (x >= 10 || y >= 10) break;
  }

  assert(y >= 10);
}