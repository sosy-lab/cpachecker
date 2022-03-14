// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int(void);

void main() {
    int data = 0;
    while (__VERIFIER_nondet_int()) {
        data = __VERIFIER_nondet_int();
    }
    if (data) {
        ERROR: goto ERROR;
    }
}
