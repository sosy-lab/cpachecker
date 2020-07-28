// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*@
    ensures x == \old(x);
    ensures y >= 0;
    behavior positive:
    assumes x >= 0;
    ensures y == x;
    behavior negative:
    assumes x < 0;
    ensures y == -x;
    complete behaviors;
    disjoint behaviors;
*/
int abs(int x) {
    int y;    
    if (x < 0) {
        y = -x;
    } else {
        y = x;
    }    
    return y;
}

int main(void) {
    abs(10);
    return 0;
}
