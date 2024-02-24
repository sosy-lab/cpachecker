// Author: heizmann@informatik.uni-freiburg.de
// Date: 2015-09-09
//
// We assume sizeof(int)=4 and sizeof(long)>4.

#include <stdio.h>

int main() {
	// The literal of type long on the right-hand side is exactly INT_MAX+1 and will 
	// be converted to int.
	// Paragraph 6.3.1.3.3 of C11 says that if "[..] the new type is signed and the 
	// value cannot be represented in it; either the result is implementation-defined 
	// or an implementation-defined signal is raised."
	int x = 2147483648L;
	printf("%d\n", x);
	return 0;
}
