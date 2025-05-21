// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_is_public(int variable, int booleanFlag);
extern int __VERIFIER_nondet_int();

int main() {
    int x = 1;
    int y = 2;
    int z = __VERIFIER_nondet_int();

    // No tainted branches are expected not to taint the return value.
    // A taint violation is expected here:
    __VERIFIER_is_public(z < 0 ? x : y, 0);
}
