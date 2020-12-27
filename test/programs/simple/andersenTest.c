// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {

    int x, y, z;
    int *p, *q;
    int **r;

    y = 42;
    x = y;
    q = malloc(x);
    p = &x;
    r = &p;
    q = &y;
    *r = q;
    r = &q;

    if (x == 42) {
        p = &z;
    }

    return (0);
}

