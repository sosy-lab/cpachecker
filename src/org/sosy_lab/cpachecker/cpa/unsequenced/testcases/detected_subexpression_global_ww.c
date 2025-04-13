#include <stdio.h>

//6.5
int r = 0;

int g(int x){
  return r = x;
}

int f(int x){
  int result = g(1)+g(2);
  return(r = x);
}

int main(){
  int result = f(1)*(f(2)+f(3)) - f(4);
  //possible r is 1,2,3,4
  printf("r = %d\n", r);
  printf("result = %d\n", result);

  return 0;
}