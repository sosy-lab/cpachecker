#include <stdio.h>
unsigned int fibonacci(unsigned char n){
  if (n <= 1) {
    return n;
  }

  // n >= 2
  int j = 2;
  int fib = 1;
  int prev[2] = {1,1};

  while(j < n){
    fib = prev[0] + prev[1];
    prev[0] = prev[1];
    prev[1] = fib;
    j++;
  }
  return fib;
}

int main(){
  int i = 0;
  while(i < 20){
    int f = fibonacci(i);
    printf("fib(%d): %d\n", i, f);
    i++;
  }
  return 0;
}
