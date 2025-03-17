#include <stdio.h>

int x = 5;
int y = 10;
int a = 1;
int b = 2;

int add(){
  int c = a + b;
  return x + y;
}

int main(){
    int result;
    result = add();
    int c = a + b;
    printf("result = %d\n", result);
    return 0;
}