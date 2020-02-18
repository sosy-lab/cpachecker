#include <stdlib.h>

void foo(int i) {
	fncArr[2] = &bar;
}

void bar(int i) {
}

void baz(int i) {
	char *a = malloc(5);
}

void (* fncArr[3])(int);

int main(int args, char** argv) {
	int y = 5;
	fncArr[0] = &bar;
	fncArr[1] = &foo;
	fncArr[2] = &baz;
	void (*p)(int) = &bar;

	int i = 0;
	for(; i < 3; i++) {
		fncArr[i](y);
	}

	p(y);

	return 0;
}