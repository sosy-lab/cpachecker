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
    int *ptr = &x;

    int z = 1;
    // point the pointer to the mem. address an untainted variable
    ptr = &z;

    __VERIFIER_is_public(*ptr, 0); // t(*ptr) = t(z) = U
    __VERIFIER_is_public(ptr, 0); // t(ptr) = t(&z) = U

    // Override the pointer should not have effect on its old value
    __VERIFIER_is_public(x, 1);
}
