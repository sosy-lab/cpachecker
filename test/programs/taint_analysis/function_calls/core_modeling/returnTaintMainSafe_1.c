// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern int __VERIFIER_is_public(int variable, int booleanFlag);

int f() {
    return __VERIFIER_nondet_int();
}

int main() {

    int a = f();
    // t(main) = U -> implicit assumption holds: __VERIFIER_is_public(main, 1);
    return 2;
}
