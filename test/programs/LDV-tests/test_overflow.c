#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

int VERDICT_UNSAFE;
/* на 32-битной архитектуре CURRENTLY_SAFE, а на 64-битной CURENTLY_UNSAFE */
int CURRENTLY_UNSAFE;

ssize_t getService();
int globalSize;

int
main(int argc, char* argv[]) {
	int retVal;
	retVal = getService();
	assert(sizeof(retVal)==globalSize);
	printf("returned value: %d\n", retVal);
	return 0;
}


ssize_t getService() {
	ssize_t localVar = 999999999999;
	globalSize = sizeof(localVar);
	printf("localVar: %d\n", localVar);
	return localVar;
}
