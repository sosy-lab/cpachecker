#include <stdlib.h>

void test(int x) {
	if (!x) {
ERROR: goto ERROR;
	}
}

int* makeIndirection(int i) {
	int si = sizeof(int);
	int* p;

	p = malloc(si);

	if (p == 0) {
END: goto END;
	}

	*p = i;
	return p;
}

void main() {
	int a = 42;
	int* p = 0;

	p = makeIndirection(a);

	test(p != 0);
	test(*p == a);
}

