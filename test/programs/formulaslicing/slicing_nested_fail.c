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

int main() {
    while (__VERIFIER_nondet_int()) {

        int a = 0;
        for (int j=0; j<10; j++) { }

        for (int j=0; j<10; j++) {
            a++;
        }
        assert(a == 0);
    }
}
