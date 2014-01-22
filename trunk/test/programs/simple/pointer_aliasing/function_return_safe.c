#include<assert.h>

struct s {
	int *p;
};

int a = 42;

int* foo() {
	return &a;
}

void main() {
	struct s s;
	int *r = &a;
	assert(*r == 42);
	assert(!(*r != 42));

	s.p = foo();
	assert (*s.p == 42);
	assert (!(*s.p != 42));
}

