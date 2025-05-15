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

    // A taint violation is expected here: The expression is tainted, and we expect it to be public
    __VERIFIER_is_public(x < 0 ? x : y, 1);
    // The order of the arguments here (`x : y` vs `y : x`) is irrelevant because:
    // case 1: the tainted branch is explored first -> an error state is reached.
    // case 2: the non-tainted branch is explored first -> no error state is reached -> the tainted branch is explored -> an error state is reached.
    // See taintByConditionalTernaryOperatorUnsafe_1_2.c
}
