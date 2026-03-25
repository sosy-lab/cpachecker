// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

int b() {
    return 0;
}

int main () {

    int (*p)();

    if (__VERIFIER_nondet_int()){
        p = main;
    } else {
        p = &b;
    }
    return p();

}