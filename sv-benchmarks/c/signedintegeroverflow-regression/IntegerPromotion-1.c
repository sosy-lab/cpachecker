// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-01

#include <stdio.h>

int main() {
	// we assume sizeof(short)=2 and sizeof(int)=4
	// assume SHRT_MAX=32768
	short a = 32767;
	// "Surprisingly", there is no overflow here. During the usual arithmetic
	// conversions, the operands of + are promoted to int.
	// The right-hand side of the assignment is converted to unsigned short
	// which is defined in 6.3.1.3.2 of C11
	// http://www.open-std.org/jtc1/sc22/wg14/www/docs/n1570.pdf
	unsigned short b = a + a + a;
	printf("value %hu",b);
	return 0;
}
