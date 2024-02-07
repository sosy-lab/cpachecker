// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

int main() {
	int u;
    int n = __VERIFIER_nondet_int();

    if (u > 10 || u < 0) {
        return 0;
    }
	
    if (n > 10 || n < 0) {
        return 0;
    }
    // n and u from 0 to 10
    if (n == u) {
        assert(!(n != u));
    } else {
        assert(!(n == u));
    }

    u = u + 1; // u from 11 to 1

    n = n - 1; // n from 9 to -1

    assert(u != 11 && n != -1);

	return 0;	
}
