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
    int z = 1;

    z = x < 0 ? y : z;

    // Despite no tainted variable is assigned to z,
    // it is expected to be tainted by implicit taint flow
    __VERIFIER_is_public(z, 0);
}
