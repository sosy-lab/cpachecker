#define  __attribute__(x) /*NOTHING*/
#include <assert.h>

// Contributed by Pavel Shved and Vadim Mutilin.
//
// This program is unsafe, and, unlike with equivalent blast_incorrect.c, BLAST detects it.
// 
// BLAST was run with the following arguments:
// pblast.opt -craig 2 -predH 7 -alias bdd -dfs -cref

typedef int rr;

rr yyyy;

// This function is not called here and is for mere comparision with blast_incorrect.c.
rr * getrr()
{
	rr * r = &yyyy;
	*r = 1;
	return r;
}

int main()
{
	rr * ptr1,*ptr2;
	//ptr1 = getrr();
	//If we comment the next line and uncomment the previous one instead
	//BLAST will not work correctly and fail with `no new predicates error'.
	ptr1 = &yyyy; *ptr1 = 1;

	ptr2 = ptr1;
	assert(*ptr2  == 1);
	*ptr2 = 2;

	ptr2 = ptr1;
	assert(*ptr2  == 1);
	*ptr2 = 2;

	return 0;
}
