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

// NOTE: Using conditional ternary operator the order matters:

    // No taint violation is expected here:
    // the expression is tainted, we expect it to be tainted, and the tainted branch is explored first
    __VERIFIER_is_public(x < 0 ? y : x, 0);

    // A taint violation is expected here:
    // the expression is tainted, and we expect it to be tainted, but the non-tainted branch is explored first
    __VERIFIER_is_public(x < 0 ? x : y, 0);
}
