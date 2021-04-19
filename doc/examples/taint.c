#include <stdio.h>
#include <string.h>
int main(void){
	static int shared = 3;
	int a, b, c, d;						// T(*) = U
	a = getchar();						// T(a) = T
	b = 10;                 			// T(b) = U
	c = a;                 				// T(c) = T
	d = b;								// T(d) = U
	c = 2;								// T(c) = U
	__VERIFIER_tainted(*c);
	printf(a);							// T(a) = T
	printf(b);							// T(b) = U
	__VERIFIER_assert_untainted(*c);	// T(c) = T
	return 0;
}
