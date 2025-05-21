// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark case extracted from project https://github.com/dceara/tanalysis/blob/master/tanalysis/tests/func_tests/

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = 2;
    int y = __VERIFIER_nondet_int();

    while (x < y) {
        x = x + y;
    }

    // Note that the problem from simpleWhileLoopSafe_3.c is not present here. In this case the analysis recognizes that the state space was completely explored.
    // x is expected to be tainted
    __VERIFIER_is_public(x, 0);
}
