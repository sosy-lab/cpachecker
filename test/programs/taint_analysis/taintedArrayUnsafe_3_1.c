// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);

int main() {
    int x = 1;
    int y = __VERIFIER_nondet_int();
    int z = 1;

    // Initialize array `d` with a non-tainted variable `x` and a tainted variable `y`
    int d[3] = {x, y};

    // Addition of a non-tainted variable to a tainted array
    d[2] = z;

    // Information-flow violation expected, because array `d` is still expected to be tainted
    __VERIFIER_is_public(d, 1);
}
