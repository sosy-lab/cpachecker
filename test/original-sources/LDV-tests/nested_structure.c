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
		} *y;
	} *x;
} Stuff;

int main()
{
	struct Innermost im = {3};
	struct Inner inner = {2, &im};
	struct Toplev good = { 1, &inner};
	good.x->y->c = 4;
	assert (good.x->y->c == 4);
	return 0;
}






