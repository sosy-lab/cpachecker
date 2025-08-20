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

    // taint y
    __VERIFIER_set_public(y, 0);

    // y's pointer is expected to be tainted
    __VERIFIER_is_public(*ptr, 0); // = y
    __VERIFIER_is_public(ptr, 0); // &y

    // sanitize the pointer `ptr`
    __VERIFIER_set_public(ptr, 1);

    // sanitizes the pointed value
    __VERIFIER_is_public(*ptr, 1); // = y
    __VERIFIER_is_public(ptr, 1); // &y
    __VERIFIER_is_public(y, 1);

    // taint the pointer
    __VERIFIER_set_public(*ptr, 0);

    // taints the pointed value
    __VERIFIER_is_public(*ptr, 0); // = y
    __VERIFIER_is_public(ptr, 0); // &y 1
    __VERIFIER_is_public(y, 0);

    int z = 1;
    // point the pointer to the mem. address an untainted variable
    ptr = &z;

    // z and its current pointer are expected to be untainted now
    __VERIFIER_is_public(*ptr, 1); // t(*ptr) = t(z) = U
    __VERIFIER_is_public(ptr, 1); // t(ptr) = t(&z) = U

    // the override of the the pointer should not have effect on its old value
    __VERIFIER_is_public(y, 0);

    // taint `z` by dereference, assigning a tainted value to it
    *ptr = x; // t(z) = t(*ptr) = t(x) = T

    // z and its current pointer are expected to be tainted now
    __VERIFIER_is_public(*ptr, 0); // = z
    __VERIFIER_is_public(ptr, 0); // &z
    __VERIFIER_is_public(z, 0);

    // sanitize x
    __VERIFIER_set_public(x, 1);

    // Sanitize of `x` should not have effect in `ptr`, since `ptr` still points to `z`.
    // z and its pointer are expected to remain tainted
    __VERIFIER_is_public(*ptr, 0);
    __VERIFIER_is_public(ptr, 0);
    __VERIFIER_is_public(z, 0);
}
