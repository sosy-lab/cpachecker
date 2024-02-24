// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-06
//
// We assume sizeof(int)=4.

#include <stdio.h>

int main() {
	int x = (65536 * -32768);
	printf("%d\n", x);
	return 0;
}
