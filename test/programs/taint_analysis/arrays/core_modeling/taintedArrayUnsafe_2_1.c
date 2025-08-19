// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = 1;
    int y = __VERIFIER_nondet_int();

    // Initialize array `d` with a non-tainted variable `x` and a tainted variable `y`
    int d[2] = {x, y};

    // Information-flow violation expected, because array `d` is now expected to be tainted
    __VERIFIER_is_public(d, 1);
}
