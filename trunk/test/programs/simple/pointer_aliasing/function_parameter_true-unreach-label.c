#include<assert.h>

void foo(int* r) {
	assert(*r == 42);
	assert(!(*r != 42));
}

void main() {
	int a = 42;
	foo(&a);
}
