// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

