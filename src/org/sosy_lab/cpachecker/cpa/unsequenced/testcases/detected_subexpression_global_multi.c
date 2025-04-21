#include <stdio.h>

int x = 0;
int y = 0;

int f() {
  x=1;
  y=2;
  return x;
}

int g() {
  y=3;
  return x;
}

int main() {
  int y = (f() + x) + g();
  return 0;
}
