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

	test(p1 != p2);

END_PROGRAM: exit();
}
