/*
 * Double free of pointer to struct.
 *
 * Use cil with --dosimpleMem flag.
 */
#include <stdlib.h>

struct str {
    int a;
    struct str* pstr;
};

int main() {
    struct str* pstruct;

    pstruct = (struct str*) malloc(sizeof(struct str));
    if (NULL == pstruct) {
	return 1;
    } 

    pstruct->pstr = (struct str*) malloc(sizeof(struct str));
    if (NULL == pstruct->pstr) {
	return 2;
    }

    free(pstruct->pstr);
    free(pstruct->pstr);

    return 0;
}

