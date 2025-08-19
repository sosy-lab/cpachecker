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
    int x = 2;
    int y = __VERIFIER_nondet_int(); // b is initialized tainted

    int z;

    if (x < 0) {
        z = y; // tainted - unreachable
    } else {
        z = x;
    }

    // No property violation expected
    __VERIFIER_is_public(z, 1);
}
