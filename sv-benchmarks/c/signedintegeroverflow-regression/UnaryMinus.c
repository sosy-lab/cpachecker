// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-09
//
// We assume sizeof(int)=4.

#include <stdio.h>

int main() {
	int minInt = -2147483647 - 1;
	int y = -minInt - 23;
	printf("%d\n", y);
	return 0;
}
