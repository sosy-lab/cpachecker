/* Structure assignment test
 */

#include <assert.h>

int VERDICT_SAFE;

typedef struct Stuff {
	int a;
	int b;
} Stuff;

int main()
{
	Stuff good = {1,2};
	Stuff bad;
	bad = good;
	assert (bad.b == 2);
	return 0;
}





