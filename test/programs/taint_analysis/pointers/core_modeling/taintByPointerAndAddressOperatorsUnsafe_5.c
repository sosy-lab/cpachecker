// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern void __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x = __VERIFIER_nondet_int(); // Tainted
    int y = 1;
    int *ptr = &y;

    // taint `y` by dereference
    *ptr = x;

    // sanitize x
    __VERIFIER_set_public(x, 1);

    // y and its current pointer are expected to be tainted now
    // and sanitize of x should not have effect on y
    __VERIFIER_is_public(*ptr, 1); // = y
    __VERIFIER_is_public(ptr, 1); // &y
    __VERIFIER_is_public(y, 1);
    __VERIFIER_is_public(x, 0);
}
