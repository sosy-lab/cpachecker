/*
 * Tests assignment to fields. Test is valid code.
 *
 * Use cil with --dosimpleMem flag.
 */
#include <stdlib.h>

struct str {
    int a;
    struct ptr* pointer;
};

struct ptr {
    int a;
};

int main() {
    struct str* pl;
    pl = (struct str*) malloc(sizeof(struct str));
    if (NULL == pl) {
	return 1;
    }

    pl->pointer = malloc(sizeof(struct ptr));
    if (NULL == pl->pointer) {
	return 2;
    }

    pl->pointer->a = 1;

    return 0; 
}
