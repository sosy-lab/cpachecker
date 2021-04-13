#include <stdio.h>
#include <string.h>
int main(void){
	int a, b, d, e;
	a = b = d = e = getchar();// all tainted
	a = 2;                  // T(a) = U
	b = 10;                 // T(b) = T
	d = a + b + d;          // T(d) = U + T + T = T
	e = a + b + b;          // T(d) = U + T + T = T
	return 0;
}
