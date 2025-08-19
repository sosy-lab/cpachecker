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
    int a, b, tainted;
    a = b = 2;
    tainted = __VERIFIER_nondet_int();

    while (tainted) {
        tainted = 3;

        while (tainted < b) { // loop-body unreachable
            b++;

            while (tainted) {
                a = tainted;
            }
        }
    }

    __VERIFIER_is_public(a, 0);
}
