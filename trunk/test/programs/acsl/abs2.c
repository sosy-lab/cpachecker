// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*@
    ensures x == \old(x);
    ensures \result >= 0;
    behavior positive:
    assumes x >= 0;
    ensures \result == x;
    behavior negative:
    assumes x < 0;
    ensures \result == -x;
    complete behaviors;
    disjoint behaviors;
*/
int abs(int x) {
    if (x < 0) {
        return -x;
    }
    return x;
}

int main(void) {
    abs(10);
    return 0;
}
