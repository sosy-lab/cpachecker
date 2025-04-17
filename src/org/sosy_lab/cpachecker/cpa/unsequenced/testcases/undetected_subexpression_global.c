#include <stdio.h>

int x = 0;
int y = 0;

int f() {
  x = 1;
  return 0;
}

int g() {
  int a = y;
  return a;
}

int main() {
  int result = f() + g(); // f() writes x, g() reads y, no conflict
  return 0;
}