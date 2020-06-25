#include <assert.h>

int main() {
	int x;

	if (x) {
		assert(!x);
		return 1;
	} else {
		assert(x);
		return 2;
	}
}
