// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    int a = 5;
    int b = 10; 
    if (a < 0) {
        b = 9;   
    } else {
        a = 20;
    }
    if (a + b < 15) {
        ERROR: return 1;
    }
    return 0;
}
