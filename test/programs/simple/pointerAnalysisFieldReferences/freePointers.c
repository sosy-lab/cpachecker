/*
 * Double free of pointer to struct.
 *
 * Use cil with --dosimpleMem flag.
 */
#include <stdlib.h>

struct str {
    struct str* ptr;
};

int main() {
    struct str* psim;
    struct str* pdp;

    psim = (struct str*) malloc(sizeof(struct str));
    if (NULL == psim) {
	return 1;
    } 

    psim->ptr = psim;

    free(psim->ptr);
    free(psim);

    return 0;
}

