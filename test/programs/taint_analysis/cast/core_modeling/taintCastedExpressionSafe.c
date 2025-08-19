// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
    int x = 1;
    int y = __VERIFIER_nondet_int();
    int z = 2;

    // No property violation expected
    __VERIFIER_is_public((char) y, 0);
    __VERIFIER_is_public((short) y, 0);
    __VERIFIER_is_public((long) y, 0);
    __VERIFIER_is_public((unsigned int) y, 0);
    __VERIFIER_is_public((unsigned char) y, 0);

    // Nested casts
    __VERIFIER_is_public((unsigned char)(short)(float)y, 0);

    // Combine nested casts with arithmetic computations
    __VERIFIER_is_public((int)((float)y * 1.5), 0);

    // Casts on arrays or memory blocks.
    int array[4] = {1, 2, 3, y};
    __VERIFIER_is_public((char)array[1], 0);

    // Test ternary operations where both branches involve a cast
    z = x < 0 ? (short)y : (unsigned int)y;
    __VERIFIER_is_public(z, 0);

    // Combine arithmetic, conditionals, and casts.
    int complex = (unsigned char)((short)y * 2 < 10 ? x + y : z - y);
    __VERIFIER_is_public(complex, 0);
}
