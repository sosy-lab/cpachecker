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

    z = y && x; // t(z) = t(y) + t(x) = U + T = T

    // This is one exception to the overapprox. that a tainted RHS taints the LHS.
    // CPAchecker parses the result of the && operation and passes it to the taint analysis
    // with that the analysis has no chance to see which variables were in the RHS.

    __VERIFIER_is_public(z, 1);
}
