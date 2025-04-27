#include <stdio.h>
int g = 0;

int f1() {
  g = 1;
  return 0;
}

int f2() {
  g = 2;
  return 0;
}
int main() {
  f1() || f2();
  return 0;
}