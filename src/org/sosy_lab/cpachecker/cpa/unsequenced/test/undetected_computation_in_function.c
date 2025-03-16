#include <stdio.h>

int x = 5;

int minus(int a, int b){
    return a-b;
}

int main(){
    int y = 10;
    int result = minus(x,y);
    printf("result = %d\n", result);
    return 0;
}