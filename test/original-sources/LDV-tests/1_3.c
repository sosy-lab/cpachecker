#include <assert.h>
#include "1_3.h"

int VERDICT_UNSAFE;
int CURRENTLY_SAFE;

/*
	getPtr returns structure rr with field state, initialized to 1
	freePtr sets field state to 2
	second call to freePtr should fail on assert(ptr -> state == 1);
*/

rr * getrr()
{
	rr * r = __undefrr();
	r -> state = 0;
	return r;
}

rr * getPtr()
{
	rr * r = getrr();
	r -> state = 1;
	return r;
}

void freePtr(rr * ptr)
{
	assert(ptr -> state == 1);
	ptr -> state = 2;
}

int main()
{
	rr * ptr1 = 0;
	ptr1 = getPtr();
	freePtr(ptr1);
	freePtr(ptr1);
	
	return 0;
}
