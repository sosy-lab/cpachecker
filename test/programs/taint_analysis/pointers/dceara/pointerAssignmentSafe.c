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
    int a, b, c;
    int *p, *q;

    c = __VERIFIER_nondet_int();
    p = __VERIFIER_nondet_int();

    // p points now to a tainted mem. address: t(p) = T
    __VERIFIER_is_public(p, 0);

    a = 2;
    p = &a;

    // p points now to an untainted memory address: t(p) = U
    __VERIFIER_is_public(p, 1);

    b = *p;

    // b contains now the value of a
    __VERIFIER_is_public(b, 1);

    *q = c;

    // The value to which q points holds now a tainted value. Or at least it should by correct initialization. In this case q hasn't been initialized
    // and therefore is the assignment invalid.
    // t(q) = U
    __VERIFIER_is_public(q, 1);

    // a and c haven't been modified
    __VERIFIER_is_public(a, 1);
    __VERIFIER_is_public(c, 0);
}
