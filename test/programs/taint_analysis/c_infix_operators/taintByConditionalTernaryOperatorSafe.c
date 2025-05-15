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

// NOTE:
// - When passing the condition expression directly to the ternary operator (see definition of `w`), the analysis starts with the outer branch (w = x).
// - When passing a `condition` variable (see definition of `z`), the analysis starts with the inner branch (z = x).
// This behavior is determined by the way CPAchecker explores the state space in conditional branches.

    int condition = x < 0;

    // In this case the inner branch (z = x) is explored first --> z tainted
    z = condition ? x : y;

    // Therefore no taint violation is expected here
    __VERIFIER_is_public(z, 0);

    // In this case the outer branch (z = x) is explored first --> w tainted
    w = x < 0 ? y : x;

    // No taint violation is expected here
    __VERIFIER_is_public(w, 0);

    // The behavior explained above applies to passing the conditional ternary operation directly to the public-state check:
    // No property violation expected
    __VERIFIER_is_public(x < 0 ? y : x, 0);
    __VERIFIER_is_public(condition ? x : y, 0);

    // There are then three situations in which we can expect a taint violation from the ternary operator:
    // 1. When the expression is tainted, and we expect it to be public (here the order of the explored branches is not relevant)
    // 2. When the expression is public, and we expect it to be tainted (here the order of the explored branches is not relevant)
    // 3. When the expression is tainted, and we expect it to be tainted, but the non-tainted branch is explored first
}
