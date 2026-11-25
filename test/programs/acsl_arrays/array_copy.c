// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);
#define SIZE 10

int main() {
    int s[SIZE] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    int d[SIZE];

    int random = __VERIFIER_nondet_int();
    __VERIFIER_assume(random >= 0);
    __VERIFIER_assume(random < SIZE);

    for (int i = 0; i < SIZE; i++) {
        d[i] = s[i];
    }

    if (d[random] != s[random]) {
        ERROR: return 1;
    }
    return 0;
}