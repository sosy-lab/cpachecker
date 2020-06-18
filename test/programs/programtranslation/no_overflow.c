// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    int x = 0;
    while (x < 10) {
        if (x % 2 == 0) {
            x = x + 3;
        } else {
            x = x + 1;
        }
    }
    int y = 2;
    if (x == 10) {
       y = 2147483647;
    }
    y = x + y;
    return 0;
}
