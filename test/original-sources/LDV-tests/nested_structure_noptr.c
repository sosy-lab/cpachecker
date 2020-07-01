
/* Complex lvalue assignment
 */

#include <assert.h>

int VERDICT_SAFE;

typedef struct Toplev {
	int a;
	struct Inner {
		int b;
		struct Innermost{
			int c;
		} y;
	} x;
} Stuff;

int main()
{
	struct Toplev good = { 1, {2, {3}}};
	good.x.y.c = 4;
	assert (good.x.y.c == 4);
	return 0;
}






