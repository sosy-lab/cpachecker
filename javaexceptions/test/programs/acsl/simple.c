// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*@
ensures x == 0;
behavior one:
assumes \true;
ensures !(x > 0);
behavior two:
assumes \true;
ensures !(x < 0);
*/
int main() {
    int x,y;
    x = 10;
    y = 20;
    while(x > 0) {
        y = y - 2;
        //@ assert y % 2 == 0;
        x--;
        //@ assert y == x * 2;
    }
    if(y) {
        ERROR: return 1;
    }
    return 0;
}
