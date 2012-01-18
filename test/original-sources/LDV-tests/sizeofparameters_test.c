#include <stdio.h>
#include <assert.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

void foo();

int globalSize;

int 
main(int argc, char* argv[]) {
	long int a;
	globalSize=sizeof(a);
	foo(a);
	return 0;
}

void foo(int a) {
	assert(sizeof(a)==globalSize);
}
