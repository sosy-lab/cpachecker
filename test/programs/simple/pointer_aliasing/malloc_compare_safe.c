#include <stdlib.h>

void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

void main() {
	int* p1;
	int* p2;
	int si = sizeof(int);

	p1 = malloc(si);
	p2 = malloc(si);

	if (p1 == 0 || p2 == 0) {
		goto END_PROGRAM;
	}

	test(p1 != p2);

END_PROGRAM: exit();
}
