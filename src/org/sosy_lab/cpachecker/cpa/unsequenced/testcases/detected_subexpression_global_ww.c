#include <stdio.h>

int x = 0;

int f() {
  x = 1;
  return 0;
}

int g() {
  x = 2;
  return 0;
}

int main() {
  // Potential unsequenced write-write conflict on 'x'
  int y = f() + g(); //Declaration
  f() + g(); //CExpressionStatement
  y = f() + g(); //CExpressionAssignmentStatement
  return f() + g(); //ReturnStatement
}