// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 1;
    int z;
    int w;

    // If the condition (here `x < 0`) cannot be evaluated. Both branches are expected to be explored.
    // If a branch taints the return value (here e.g., `z`), we call it a "tainted branch".
    // There is at least one tainted branch, so is z expected to be tainted after the branches exploration.

    // One tainted branch and one untainted branch.
    z = x < 0 ? x : y;
    __VERIFIER_is_public(z, 0);

    // Result expected to be consistent, regardless of the order of the branch exploration
    w = x < 0 ? y : x;
    __VERIFIER_is_public(w, 0);

    // Passing the conditional ternary operation directly to the public-state check is expected to behave exactly as above:
    // No property violation expected
//    __VERIFIER_is_public(x < 0 ? x : y, 0); // tmp = y explored first
//    __VERIFIER_is_public(x < 0 ? y : x, 0); // tmp = x explored first

    // No tainted branches. Result expected to be public
    __VERIFIER_is_public(x < 0 ? y : y, 1);

    // Only tainted branches. Result expected to be tainted
    __VERIFIER_is_public(x < 0 ? x : x, 0);
}
