#include <stdio.h>

int x = 5;
int y = 10;

int minus(){
    return x-y;
}

int main(){
    int result = minus();
    printf("result = %d\n", result);
    return 0;
}