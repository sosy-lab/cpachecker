#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

struct miniStruct {
	int a;
	int b;
};

int
main(int argc, char* argv[]) {
	struct miniStruct *minis;
	minis = malloc(sizeof(minis));
	assert(sizeof(minis) == sizeof(struct minis));
	return 0;
}
