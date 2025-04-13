#include <stdio.h>

int minus(int a, int b){
    return a-b;
}

int main(){
    int y = 10;
    int result = minus(5,y);
    printf("result = %d\n", result);
    return 0;
}