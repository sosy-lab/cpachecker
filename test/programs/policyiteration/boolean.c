// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2014-2017 Université Grenoble Alpes
//
// SPDX-License-Identifier: Apache-2.0

void assert(int cond) { if (!cond) { ERROR: return; } }

extern int __VERIFIER_nondet_int();
extern void __VERIFIER_assume(int);

const int BOUND = 1000;

int main () {
    int test = __VERIFIER_nondet_int();
    int sum = 0;
    for (int i=0; i<BOUND; i++) {
        if (test) {
            sum++;
        }
    }
    assert(sum == BOUND || sum == 0);
}
