#include <assert.h>
#include <malloc.h>

#ifdef BLAST_AUTO_1
/* using malloc */
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;
#else
/* using separate variables */
int VERDICT_SAFE;
int CURRENTLY_UNSAFE;
#endif

int main(void) {
#ifdef BLAST_AUTO_1
	int * p1 = malloc(sizeof(int));
	int * p2 = malloc(sizeof(int));
#else
	int a,b;
	int *p1=&a;
	int *p2=&b;
#endif
	if(p1!=0 && p2!=0) {
		assert(p1!=p2);
	}
	return 0;
}
