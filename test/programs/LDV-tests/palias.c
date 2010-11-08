int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

#include <assert.h>
int main(void) {
	int *a,*b;
	b = a;
	*a = 2;
	assert(*b==2);
	return 0;
}
