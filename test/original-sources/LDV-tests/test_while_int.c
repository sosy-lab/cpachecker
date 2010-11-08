#include <assert.h>

#ifdef BLAST_AUTO_1
int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;
#else

int VERDICT_UNSAFE;
int CURRENTLY_UNSAFE;

//special assert
void check_error(int b) {
	assert(b);
}
#endif

int main(void) {
	int i=0;
        while(i<5) {
                i++;
#ifdef BLAST_AUTO_1
                assert(i!=3);
#else
		check_error(i!=3);
#endif
        }
	return 0;
}
