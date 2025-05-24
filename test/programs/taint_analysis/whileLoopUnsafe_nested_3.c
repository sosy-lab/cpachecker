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
        a = tainted; // t(a) = t(tainted) = T

        while (tainted < b) {
            b++;
            tainted = b; // t(tainted) = U
        }
        // t(tainted) = T + U = T
    }

    // t(a) = t(tainted) = T + U = T
    // `tainted` could become untainted, but the overall joined status is still tainted
    // a is expected to be tainted
    __VERIFIER_is_public(a, 1);
}
