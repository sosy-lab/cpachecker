#include <assert.h>

void f() {
};

extern int constant();

void main() {
	int i1;
	int i2;
	i1 = constant();
	f();
	i2 = constant();
	assert(i1 == i2);
}
