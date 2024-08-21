// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main() {
    int x = 1;
    int y;
    switch(x) {
     case 0:
        y = 1;
        break;
     case 1:
        y = 0;
        break;
     default:
        y = -1;
    }
    return y;
}
