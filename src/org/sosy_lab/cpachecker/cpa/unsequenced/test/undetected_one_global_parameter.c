#include <stdio.h>

int x = 5;


int minus(int a, int b){
  return a - b;
}

int main(){
    int result;
    result = minus(x,10);
    printf("result = %d\n", result);
    return 0;
}