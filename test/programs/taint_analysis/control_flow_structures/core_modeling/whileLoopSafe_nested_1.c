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
        tainted++;

        while (tainted < b) {
            b++;              // t(b) = t(tainted) + t(b) = T +U = T

            while (tainted) {
                a = tainted;  // t(a) = t(tainted) + t(b) = T + T = T
            }
        }
    }

    // t(a) = T -> implicit taint propagation
    __VERIFIER_is_public(a, 0);

    // t(b) = T -> explicit taint propagation
    __VERIFIER_is_public(b, 0);
}
