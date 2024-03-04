// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();
int main() {
    int x;
    int y = 0;
    while (x = __VERIFIER_nondet_int()) {
    }
    // this check is necessary so that symbolic execution _with_ CEGAR
    // tracks x
    if (x) {
      goto ERROR;
    }
    return 0;
ERROR:
    return -1;
}
