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

    if (u > 10 || u < 10) {
        return 0;
    }
	
    if (n > 10 || n < 10) {
        return 0;
    }
    // n == u == 10
    assert(u == n);
    assert(!(u != n));

    u = u + 1;

    n = n - 1;

    assert(u != n);
    assert(!(u == n));

    assert(u == n + 2);

    int newu = u * 2;

    int newn = n * 2;

    assert(newu != newn);

    assert(newu == newn + 4);

	return 0;	
}
