#include <assert.h>

#ifdef BLAST_AUTO_1
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;
#else
int VERDICT_SAFE;
int CURRENTLY_SAFE;
#endif


union A {
	int list;
	int l2;
	char * str;
};

int main(void) {
	union A x;
	x.list = 0;
#ifdef BLAST_AUTO_1
	assert(x.l2 == 0);
#else
	assert(x.list==0);
#endif
}
