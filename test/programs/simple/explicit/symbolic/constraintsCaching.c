// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_error();

int main() {
    int a = __VERIFIER_nondet_int();
    int b = a + 5;

    if (a > 0) {
        a = a - 1;
    }

    if (b > -4) {
ERROR:
        __VERIFIER_error();
        return -1;
    }
}
