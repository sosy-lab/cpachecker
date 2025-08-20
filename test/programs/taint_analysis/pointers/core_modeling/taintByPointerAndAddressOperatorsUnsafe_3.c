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
    int y = 1;
    int *ptr = &y;

    __VERIFIER_set_public(*ptr, 0);

    __VERIFIER_is_public(*ptr, 1); // = y
    __VERIFIER_is_public(ptr, 1); // &y 1
    __VERIFIER_is_public(y, 1);
}
