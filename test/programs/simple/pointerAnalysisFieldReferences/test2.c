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

