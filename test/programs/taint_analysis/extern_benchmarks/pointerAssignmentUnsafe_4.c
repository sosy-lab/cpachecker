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
    int c = __VERIFIER_nondet_int();;
    int *q;

    *q = c;

    // The value to which q points holds now a tainted value. Or at least it should by correct initialization. In this case q hasn't been initialized
    // and therefore is the assignment invalid.
    // t(q) = U
    __VERIFIER_is_public(q, 0);
}
