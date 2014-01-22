#define  __attribute__(x) /*NOTHING*/
#include <assert.h>

// Contributed by Pavel Shved and Vadim Mutilin.
//
// This program is unsafe, but BLAST reports a `No new predicates' error.
// 
// BLAST was run with the following arguments:
// pblast.opt -craig 2 -predH 7 -alias bdd -dfs -cref

typedef int rr;

rr yyyy;

rr * getrr()
{
	rr * r = &yyyy;
	*r = 1;
	return r;
}

int main()
{
	rr * ptr1,*ptr2;
	ptr1 = getrr();
	//If we uncomment the next line, BLAST will work correctly and report unsafety.
	//ptr1 = &yyyy; *ptr1 = 1;

	ptr2 = ptr1;
	assert(*ptr2  == 1);
	*ptr2 = 2;

	ptr2 = ptr1;
	assert(*ptr2  == 1);
	*ptr2 = 2;

	return 0;
}
