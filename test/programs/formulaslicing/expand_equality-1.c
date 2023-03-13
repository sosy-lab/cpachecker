// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void assert(int cond) { if (!cond) { ERROR: __VERIFIER_error(); } }

extern int __VERIFIER_nondet_int();

int main() {
    int i = 1;
    for (int k=0; k<__VERIFIER_nondet_int(); k++) {
        i++;
    }
    assert(i >= 0);
}
