// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern int __VERIFIER_nondet_int();

int main() {
    int x = __VERIFIER_nondet_int();
    int y = 10;
    if (x + y == 10) {
        y = y - x;
    } else {
        y = y + x;
    }
    if (y < 0) {
        y = y * (-1);
    }
    if (y < 0) {
        goto ERROR;
    }
    return 0;
ERROR: return 1;
}
