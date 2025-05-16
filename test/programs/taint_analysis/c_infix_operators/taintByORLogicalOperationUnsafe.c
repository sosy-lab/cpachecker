// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_set_public(int variable, int booleanFlag);
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int main() {
    int x, y, z;
    x = __VERIFIER_nondet_int();
    y = 1;

    // z is expected to be public (explanation in taintByANDLogicalOperationUnsafe.c)
    z = y || x;

    // taint violation expected
    __VERIFIER_is_public(z, 0);
}
