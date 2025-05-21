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

    // NOTE: Currently the analysis can not handle this loop. It does not terminate when the public-status check holds, because the condition can not be evaluated,
    // and, therefore, it:
    // 1. explores the loop body
    // 2. then explores the branch outside the loop
    // 3. Then goes to the public-status check and does not find any property violation
    // 4. Then it checks the loop body again... repeats...
    while (x < y) {
        x++;
    }
    // The analysis is failing to recognize that all options were already explored

    // x is expected to be public
    __VERIFIER_is_public(x, 1);
}
