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
    int z = 1;

    // Initialize array `d` with two non-tainted variables `x` and `z` and a tainted variable `y`
    int d[3] = {x, y, z};

    // `d` and its elements are expected to be tainted
    __VERIFIER_is_public(d, 0);
    for (int i = 0; i < sizeof(d) / sizeof(d[0]); i++) {
        __VERIFIER_is_public(d[i], 1);
    }

    // Information-flow violation expected:
    // Despite the array `d` and its contained values being tainted, adding `x`,
    // `y` and `z` to the tainted array `d` should not modify their original taint state.
    __VERIFIER_is_public(z, 0);
}
