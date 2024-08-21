// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * Assigning to invalid pointer.
 *
 * Use cil with --dosimpleMem flag.
 */
#include <stdlib.h>

struct node {
    int val;
    struct node* next;
};

int main() {
    struct node* fst;
    struct node* tmp;
    int temp;

    fst = (struct node*) malloc(sizeof(struct node));

    if (NULL == fst) {
	return 1;
    }

    tmp = malloc(sizeof(struct node));
    fst->next = tmp;

    if (NULL == tmp) {
	return 1;
    }

    fst->next->val = 1; // correct
    fst->next->next->val = 1; // error!

    return 0;
}

