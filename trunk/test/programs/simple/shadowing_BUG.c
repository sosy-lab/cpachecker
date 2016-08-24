#include <assert.h>

int x = 5;

void check_global() {
	assert(x == 3);
}

void main() {
	int x;
	x = 3;
	assert(x == 3);
	check_global();
}
