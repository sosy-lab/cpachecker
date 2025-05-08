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
    int x = __VERIFIER_nondet_int();
    int y = 1;
    int z;

    // z is expected to be tainted by x and the + operation
    z = y + x;
    __VERIFIER_is_public(z,0);

    // Sanitize z
    __VERIFIER_set_public(z,1);

    // z is expected to be tainted by x and the - operation
    z = y - x;
    __VERIFIER_is_public(z,0);

    // Sanitize z
    __VERIFIER_set_public(z,1);

    // z is expected to be tainted by x and the * operation
    z = y * x;
    __VERIFIER_is_public(z,0);

    // Sanitize z
    __VERIFIER_set_public(z,1);

    // z is expected to be tainted by x and the / operation
    z = y / x;
    __VERIFIER_is_public(z,0);

    // Sanitize z
    __VERIFIER_set_public(z,1);

    // z is expected to be tainted by x and the % (modulo) operation
    z = y % x;
    __VERIFIER_is_public(z,0);

    // TODO: make the taint analysis recognize:
    // __VERIFIER_is_public(x + y,0);
    // as tainted. Meaning, not only when one variable is contained in the first argument,
    // but when the first argument is an expression, the taint analysis should be able to
    // recognize that expression as tainted, by identifying whether the expression contains
    // tainted variables.
}
