// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int wif() {
    return 4;
}

int main(void) {
    int x;
    int y;
    {
        x = 0;
        y = 0;
    }
    for (int i = 0; i < 20; i++)
    {
        ({int a = y; wif();});
        x = ({int a = y; x + a;});
        if (0) {
          //@ assert \false;
          return 2;
        }
        y = 0;
        /*@ ensures y > 0;
        */
        for (int j = i; j < 20; j++) 
            y++;
         wif();if (x == y)
           return 1;
    }
    return 0;
}
