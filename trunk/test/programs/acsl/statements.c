// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    //@ ensures x == 0;
    int x = 0;
    //@ ensures y == 0;
    int y = 0;
    for (int i = 0; i < 20; i++) {
        /*@ ensures x == i; */
        x = i;
        /*@ requires x == i; ensures y == i; */
        y = x;
    }
    return 0;
}
