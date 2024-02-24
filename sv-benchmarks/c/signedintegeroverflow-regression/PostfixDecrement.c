// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-09
//
// We assume sizeof(int)=4.

#include <stdio.h>

int main() {
	int x = -2147483647 - 1;
	x--;
	printf("%d\n", x);
	return 0;
}
