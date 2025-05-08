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
    int x = __VERIFIER_nondet_int(); // Tainted
    int y = 10;
    int z;

// TODO: case for fasle positive:
// (we use the language of taint analysis for false positives or negatives)
//    int condition0 = (y == 10);
//    z = condition0 ? 0 : x;
//
//    __VERIFIER_is_public(z, 1);

    int condition = x < 0;

    // z is expected to be tainted by x and the conditional operator
    z = condition ? x : y;

// TODO: benchmark case: mention the current file as explanation for the taint behavior
//    if (xGreaterThatNull) {
//      z = x; // tainted
//    } else {
//      z = y; // not tainted
//    }

    __VERIFIER_is_public(z, 0);
}
