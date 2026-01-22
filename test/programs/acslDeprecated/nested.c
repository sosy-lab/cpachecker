// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(void) {
    int x = 0;
    int y = 0;
    /*@ behavior one:
            assumes i < 10;
            ensures x < 200;
        behavior two:
            assumes i >= 10;
            ensures x < 1000;
    */
    for (int i = 0; i < 20; i++) {
        x += y;
        y = 0;
        /*@ for two:
                requires j > 9;
                ensures y < 11;
        */
        for (int j = i; j < 20; j++) {
            y++;
        }    
    }
    return 0;
}
